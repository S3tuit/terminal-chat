package org.chatq.chat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat/{username}/{chatId}")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ChatSocket {

    private final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    @Inject
    ChatSseResource chatSseResource;

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        sessionMap.put(username, session);
        this.streamActiveUsernames();
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String username, @PathParam("chatId") String chatId) {
        ObjectId chatIdObj;
        try{
            chatIdObj = new ObjectId(chatId);
        } catch (Exception ex){
            ex.printStackTrace();
            return;
        }

        ChatMessage chatMessage = new ChatMessage(username, message, Instant.now(), chatIdObj);
        chatMessage.persist();
        // custom converter to a valid json string
        this.broadcast(chatMessage.toJsonNoChatId());
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        sessionMap.remove(username);
        this.streamActiveUsernames();
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable) {
        sessionMap.remove(username);
        throwable.printStackTrace();
        this.streamActiveUsernames();
    }

    private void broadcast(String chatMessage) {

        sessionMap.values().forEach(session -> session.getAsyncRemote().sendObject(chatMessage, sendResult -> {
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
