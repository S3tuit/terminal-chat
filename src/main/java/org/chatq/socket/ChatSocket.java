package org.chatq.socket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.bson.types.ObjectId;
import org.chatq.ChatSseResource;
import org.chatq.entities.ChatMessage;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat/{username}")
@ApplicationScoped
public class ChatSocket {

    private final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    @Inject
    ChatSseResource chatSseResource;

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        sessionMap.put(username, session);
        this.streamActiveUsernames();
        this.sendMessage(String.format("User %s joined the chat!", username));
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String username) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.fromUsername = username;
        chatMessage.message = message;
        chatMessage.timestamp = Instant.now();
        chatMessage.persist();
        System.out.println(chatMessage.message);
        this.sendMessage(String.format(">> %s: %s", username, message));
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        sessionMap.remove(username);
        this.streamActiveUsernames();
        this.sendMessage(String.format("User %s left the chat!", username));
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable) {
        sessionMap.remove(username);
        throwable.printStackTrace();
        this.streamActiveUsernames();
        this.sendMessage(String.format("User %s is facing some issue :(...", username));
    }

    private void sendMessage(String message) {
        sessionMap.values().forEach(session -> session.getAsyncRemote().sendObject(message, sendResult -> {
            if (sendResult.getException() != null) {
                sendResult.getException().printStackTrace();
            }
        }));
    }

    public void streamActiveUsernames() {
        chatSseResource.streamActiveUsernames(this.getActiveUsernames());
    }

    public Set<String> getActiveUsernames() {
        return sessionMap.keySet();
    }
}
