package org.chatq.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.chatq.auth.AuthService;
import org.chatq.users.UserRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat/ws")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ChatSocket {

    // Maps the sessionId to the actual Session
    ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    @Inject
    AuthService authService;
    @Inject
    UserRepository userRepository;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    SessionRepository sessionRepository;
    @Inject
    ChatSessionRepository chatSessionRepository;

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
            sessionMap.put(session.getId(), session);

            this.storeSession(session.getId(), username)
                    .flatMap(everythingOk -> {
                        if (!everythingOk) {
                            return closeSessionReactive(session, CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Oh no... something went wrong during validation");
                        }
                        // Session stored successfully
                        System.out.println("Session " + session.getId() + " opened");
                        return Uni.createFrom().voidItem();
                    })
                    .onFailure().invoke(th -> {System.out.println("failedddddd " + th.getMessage());})
                    .subscribe().with(result -> {
                        System.out.println("Store session result: " + result);
                    }, failure -> {
                        failure.printStackTrace();
                    });

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


    // return true if everything was added to Redis correctly
    public Uni<Boolean> storeSession(String sessionId, String username) {
        return userRepository.getChatIds(username)
                .flatMap(chatIds -> {

                    // Store the session and the user data
                    return sessionRepository.storeSession(sessionId, username, chatIds)
                            .flatMap(valuesStored -> {
                                if (valuesStored > 0 && chatIds != null && !chatIds.isEmpty()) {

                                    // For each chatId, add the current session to the available ones for that chat
                                    return Multi.createFrom().iterable(chatIds)
                                            .onItem().transformToUni(chatId ->
                                                    chatSessionRepository.storeAvailableSessionForChat(chatId, username)
                                            ).concatenate().collect().asList().replaceWith(true);
                                } else {
                                    return Uni.createFrom().item(false);
                                }
                            });
                })
                .onFailure().recoverWithItem(th -> {
                    th.printStackTrace();
                    return false;
                });
    }

    private Uni<Void> closeSessionReactive(Session session, CloseReason.CloseCodes code, String message) {
        return Uni.createFrom().voidItem()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .invoke(() -> {
                    try {
                        session.close(new CloseReason(code, message));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
    }

    @OnMessage
    // Expecting a JSON containing {chatId: String, message: String}
    public Uni<Void> onMessage(Session session, String incomingMsg) {
        try{
            ChatMessage chatMessage = objectMapper.readValue(incomingMsg, ChatMessage.class);
            return sessionRepository.getValueFromSession(session.getId(), "username")
                            .onItem().ifNotNull().transformToUni(username -> {
                                chatMessage.fromUsername = username;
                                chatMessage.timestamp = Instant.now();
                                if (chatMessage.isValid()) {
                                    return this.broadcast(chatMessage);
                                }
                                return Uni.createFrom().voidItem();
                    })
                    .onItem().ifNull().continueWith(() -> {
                        System.err.println("Username not found for session: " + session.getId());
                        return null;
                    });

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Uni.createFrom().voidItem();
        }
    }

    @OnClose
    public Uni<Void> onClose(Session session) {
        return sessionRepository.removeSession(session.getId())
                .invoke(() -> sessionMap.remove(session.getId()))
                .replaceWithVoid()
                .onFailure().invoke(failure -> {
                    System.err.println("Failed to remove session from Redis: " + failure.getMessage());
                });
    }

    @OnError
    public Uni<Void> onError(Session session, Throwable throwable) {
        return sessionRepository.removeSession(session.getId())
                .invoke(() -> sessionMap.remove(session.getId()))
                .replaceWithVoid()
                .onFailure().invoke(failure -> {
                    System.err.println("Failed to remove failed session from Redis: " + failure.getMessage());
                });
    }

    private Uni<Void> broadcast(ChatMessage chatMessage) {

        return chatSessionRepository.getAvailableSessionsForChat(chatMessage.chatId)
                .flatMap(sessionIds -> {
                    // for each sessionId find the actual Session stored in-memory
                    for (String sessionId : sessionIds) {
                        Session currSession = sessionMap.get(sessionId);
                        if (currSession != null) {
                            // Custom converter because ObjectMapper doesn't work well with Instant and ObjectId
                            currSession.getAsyncRemote().sendText(chatMessage.toJson());
                        } else {
                            // If session is not in-memory, remove it from redis
                            return chatSessionRepository.removerSessionFromChat(chatMessage.chatId, sessionId)
                                    .replaceWithVoid();
                        }
                    }
                    return Uni.createFrom().voidItem();
                });

    }

}
