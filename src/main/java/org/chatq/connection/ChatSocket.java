package org.chatq.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.chatq.auth.AuthService;
import org.chatq.chat.ChatMessage;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket(path = "/chat/ws/{token}")
@ApplicationScoped
public class ChatSocket {

    // Maps the username to the actual WebSocketConnection
    ConcurrentHashMap<String, WebSocketConnection> connectionMap = new ConcurrentHashMap<>();

    @Inject
    AuthService authService;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    ConnectionRepository connectionRepository;


    @OnOpen
    public Uni<Void> onOpen(WebSocketConnection connection) {

        System.out.println("Trying to connect: " + connection.id());
        // Check for token validity
        String token = connection.pathParam("token");
        if (token == null || token.isEmpty()) {
            return connection.close(new CloseReason(CloseReason.NORMAL.getCode(), "Hmm, forgot your token?"));
        }

        // Check for user permission
        String username = authService.getUsernameIfPermission(token);
        if (username == null) {
            return connection.close(new CloseReason(CloseReason.NORMAL.getCode(), "Hold on, bro, limited zone"));
        }
        connectionMap.put(username, connection);

        return connectionRepository.storeConnection(connection.id(), username)
                .flatMap(everythingOk -> {
                    if (!everythingOk) {
                        System.out.println("Failed to connect: " + connection.id());
                        return connection.close(new CloseReason(CloseReason.INTERNAL_SERVER_ERROR.getCode(),
                                "Oh no... something went wrong during validation"));
                    }

                    System.out.println("All good: " + connection.id());
                    return Uni.createFrom().voidItem();
                })
                .onFailure().invoke(th -> {
                    System.out.println("Error while storing a connection " + th.getMessage());
                });
    }


    @OnTextMessage
    // Expecting a JSON containing {chatId: String, message: String}
    public Uni<Void> onMessage(WebSocketConnection connection, String incomingMsg) {
        try {
            ChatMessage chatMessage = objectMapper.readValue(incomingMsg, ChatMessage.class);

            // retrieve the username associated with the connection
            return connectionRepository.getValueFromConnection(connection.id(), "username")
                    .onItem().ifNotNull().transformToUni(username -> {
                        // Completes the ChatMessage with missing data
                        chatMessage.fromUsername = username;
                        chatMessage.timestamp = Instant.now();

                        if (chatMessage.messageValidity()) {
                            return chatMessage.persist()
                                    .flatMap(ignored ->
                                            (this.broadcast(chatMessage))
                                    );
                        }
                        return Uni.createFrom().voidItem();
                    })
                    .replaceWithVoid()
                    .onFailure().invoke(e -> {
                        e.printStackTrace();
                    });

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Uni.createFrom().voidItem();
        }
    }

    @OnClose
    public Uni<Void> onClose(WebSocketConnection connection) {
        System.out.println("Closing connection: " + connection.id());

        return connectionRepository.getValueFromConnection(connection.id(), "username")
                .flatMap(username -> {
                    if (username != null) {
                        connectionMap.remove(username);
                        return connectionRepository.removeConnection(connection.id(), username);
                    } else {
                        System.err.println("Closed connection not found in Redis, connectionId: " + connection.id());
                        return Uni.createFrom().voidItem();
                    }
                })
                .onFailure().invoke(failure ->
                        System.err.println("Failed to remove connection from Redis: " + failure.getMessage())
                );
    }

    @OnError
    public Uni<Void> onError(WebSocketConnection connection, Throwable throwable) {
        System.out.println("Error in connection: " + connection.id() + "\n" + throwable.getMessage());
        return connectionRepository.getValueFromConnection(connection.id(), "username")
                .flatMap(username -> {
                    if (username != null) {
                        connectionMap.remove(username);
                        return connectionRepository.removeConnection(connection.id(), username);
                    } else {
                        System.err.println("Erroneous connection not found in Redis, connectionId: " + connection.id());
                        return Uni.createFrom().voidItem();
                    }
                })
                .onFailure().invoke(failure ->
                        System.err.println("Failed to remove erroneous from Redis: " + failure.getMessage())
                );
    }

    private Uni<Void> broadcast(ChatMessage chatMessage) {

        return connectionRepository.getAvailableUsernamesForChat(chatMessage.chatId)
                .onItem().ifNotNull().transformToUni(usernames ->

                        // for each connectionId find the actual Connection stored in-memory
                        Multi.createFrom().iterable(usernames)
                                .onItem().transformToUni(username -> {
                                    WebSocketConnection currConnection = connectionMap.get(username);
                                    System.out.println("Processing connection for user: " + username);

                                    // If the connection is present in-memory, broadcast to it
                                    if (currConnection != null) {
                                        // Custom converter because ObjectMapper doesn't work well with Instant and ObjectId
                                        return currConnection.sendText(chatMessage.toJson());
                                    } else {
                                        // If connection is not in-memory, remove it from redis
                                        return connectionRepository.removerUsernameFromChat(chatMessage.chatId, username)
                                                .replaceWithVoid();
                                    }
                                })
                                .merge().collect().asList()
                                .replaceWithVoid()
                );
    }

}
