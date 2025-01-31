package org.chatq.socket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat/{username}")
@ApplicationScoped
public class ChatSocket {

    private final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        sessionMap.put(username, session);
        this.sendMessage(String.format("User %s joined the chat!", username));
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String username) {
        this.sendMessage(String.format(">> %s: %s", username, message));
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        sessionMap.remove(username);
        this.sendMessage(String.format("User %s left the chat!", username));
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable) {
        sessionMap.remove(username);
        throwable.printStackTrace();
        this.sendMessage(String.format("User %s is facing some issue :(...", username));
    }

    private void sendMessage(String message) {
        sessionMap.values().forEach(session -> session.getAsyncRemote().sendObject(message, sendResult -> {
            if (sendResult.getException() != null) {
                sendResult.getException().printStackTrace();
            }
        }));
    }
}
