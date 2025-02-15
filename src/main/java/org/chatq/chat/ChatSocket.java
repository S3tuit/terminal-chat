package org.chatq.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import org.chatq.auth.AuthService;
import org.chatq.users.User;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat/ws")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ChatSocket {

    private final ConcurrentHashMap<Session, String> sessionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> onlineUserMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> activeUserMap = new ConcurrentHashMap<>();

    @Inject
    AuthService authService;
    @Inject
    ObjectMapper objectMapper;

    @OnOpen
    public void onOpen(Session session) {
        try {
            // Check for token validity
            List<String> tokenList = session.getRequestParameterMap().get("token");
            if (tokenList == null || tokenList.isEmpty()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Hmm, forgot your token?"));
                return;
            }
            String token = tokenList.getFirst();

            // Check for user permission
            String username = authService.getUsernameIfPermission(token);
            if (username == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Hold on, bro, limited zone"));
                return;
            }

            this.sessionMap.put(session, username);
            this.addUsernameToUserMap(username);


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

    private void addUsernameToUserMap(String username) {
        for (ObjectId chatIdObj : User.getChatIds(username)) {
            onlineUserMap.computeIfAbsent(chatIdObj.toString(), set -> new HashSet<>())
                    .add(username);
        }
    }

    private void removeUsernameToUserMap(String username) {
        for (ObjectId chatIdObj : User.getChatIds(username)) {
            onlineUserMap.computeIfPresent(chatIdObj.toString(), (k, userSet) -> {
                userSet.remove(username);
                return userSet.isEmpty() ? null : userSet;
            });
        }
    }

    @OnMessage
    // Expecting a JSON containing {chatId: String, message: String}
    public void onMessage(Session session, String incomingMsg) {
        try{
            ChatMessage chatMessage = objectMapper.readValue(incomingMsg, ChatMessage.class);
            chatMessage.timestamp = Instant.now();
            if (chatMessage.isValid()) {
                chatMessage.persist();
                this.broadcast(chatMessage);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        try {
            String username = sessionMap.remove(session);
            this.removeUsernameToUserMap(username);
        } catch (NullPointerException e) {
            System.out.println("We chouldn't close the websocket session");
        }

    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        try {
            String username = sessionMap.remove(session);
            this.removeUsernameToUserMap(username);
        } catch (NullPointerException e) {
            System.out.println("We chouldn't close the failed websocket session");
        }
        System.out.println("Error in the chat socket");
        throwable.printStackTrace();
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

    private void broadcast(ChatMessage chatMessage) {

        sessionMap.forEachKey(1, session -> session.getAsyncRemote().
                sendText(chatMessage.toJson(), sendResult -> {
                    if (sendResult.getException() != null) {
                        sendResult.getException().printStackTrace();
                    }
                }));

    }

    private void broadcastOnlineUsernames(Set<String> usernames) {}

}
