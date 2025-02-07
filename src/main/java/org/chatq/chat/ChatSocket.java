package org.chatq.chat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.chatq.auth.AuthService;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat/{chatId}")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ChatSocket {

    private final ConcurrentHashMap<Session, String> sessionMap = new ConcurrentHashMap<>();

    @Inject
    ChatSseResource chatSseResource;
    @Inject
    AuthService authService;

    @OnOpen
    public void onOpen(Session session, @PathParam("chatId") String chatId) {
        try {
            // Check for token validity
            List<String> tokenList = session.getRequestParameterMap().get("token");
            if (tokenList == null || tokenList.isEmpty()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Hmm, forgot your token?"));
                return;
            }
            String token = tokenList.getFirst();

            // Check if the @PathParam("chatId") is a valid Chat entity at db
            ObjectId chatIdObj = this.castChatId(chatId);
            if (chatIdObj == null || Chat.getChatIdIfExists(chatIdObj) == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "That's surely not a valid chat"));
                return;
            }

            // Check for user permission
            String username = authService.getUsernameIfPermission(token, chatIdObj);
            if (username == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Hold on, bro, limited zone"));
                return;
            }
            sessionMap.put(session, username);
            this.streamActiveUsernames();

        } catch (Exception e) {
            // If something goes wrong, close session
            e.printStackTrace();
            try{
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                        "Oh no... something went wrong during validation"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @OnMessage
    public void onMessage(Session session, String message, @PathParam("chatId") String chatId) {
        ObjectId chatIdObj = this.castChatId(chatId);
        if (chatIdObj == null) {
            return;
        }

        String username = sessionMap.get(session);
        ChatMessage chatMessage = new ChatMessage(username, message, Instant.now(), chatIdObj);
        chatMessage.persist();
        // chatMessage.toJsonNoChatId() is a custom converter to a valid json string
        this.broadcast(chatMessage.toJsonNoChatId());
    }

    @OnClose
    public void onClose(Session session) {
        sessionMap.remove(session);
        this.streamActiveUsernames();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessionMap.remove(session);
        System.out.println("Error in the chat socket");
        throwable.printStackTrace();
        this.streamActiveUsernames();
    }

    private ObjectId castChatId(String chatId){
        ObjectId chatIdObj;
        try{
            chatIdObj = new ObjectId(chatId);
            return chatIdObj;
        } catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    private void broadcast(String chatMessage) {

        sessionMap.forEachKey(1, session -> session.getAsyncRemote().
                sendObject(chatMessage, sendResult -> {
                    if (sendResult.getException() != null) {
                        sendResult.getException().printStackTrace();
                    }
                }));

    }

    public void streamActiveUsernames() {
        chatSseResource.streamActiveUsernames(this.getActiveUsernames());
    }

    public Collection<String> getActiveUsernames() {
        return sessionMap.values();
    }
}
