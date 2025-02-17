package org.chatq.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
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

    // Stores the session and the username
    private final ConcurrentHashMap<Session, String> sessionUsernameMap = new ConcurrentHashMap<>();
    // Stores the chatIds and the current sessions that have access to that chat
    private final ConcurrentHashMap<ObjectId, Set<Session>> onlineUserMap = new ConcurrentHashMap<>();

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

            this.sessionUsernameMap.put(session, username);
            this.addUsernameToSessionUserMap(username, session);


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

    private void addUsernameToSessionUserMap(String username, Session session) {
        for (ObjectId chatIdObj : User.getChatIds(username)) {
            onlineUserMap.computeIfAbsent(chatIdObj, set -> new HashSet<>())
                    .add(session);
        }
    }

    private void removeUsernameToUserMap(String username, Session session) {
        for (ObjectId chatIdObj : User.getChatIds(username)) {
            onlineUserMap.computeIfPresent(chatIdObj, (k, userSet) -> {
                userSet.remove(session);
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
            chatMessage.fromUsername = sessionUsernameMap.get(session);
            if (chatMessage.isValid()) {
                chatMessage.persist();
                this.broadcast(chatMessage);
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        try {
            String username = sessionUsernameMap.remove(session);
            this.removeUsernameToUserMap(username, session);
        } catch (NullPointerException e) {
            System.out.println("We couldn't close the websocket session");
        }

    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        try {
            String username = sessionUsernameMap.remove(session);
            this.removeUsernameToUserMap(username, session);
        } catch (NullPointerException e) {
            System.out.println("We couldn't close the failed websocket session");
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

        for (Session session : onlineUserMap.get(chatMessage.chatId)) {
            session.getAsyncRemote().
                    sendText(chatMessage.toJson(), sendResult -> {
                        if (sendResult.getException() != null) {
                            sendResult.getException().printStackTrace();
                        }
                    });
        }

    }

}
