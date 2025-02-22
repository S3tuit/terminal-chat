package org.chatq.connection;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.chatq.users.UserRepository;

import java.util.Set;

@ApplicationScoped
public class ConnectionRepository {

    @Inject
    UserRepository userRepository;
    @Inject
    ConnectionUserMap connectionUserMap;
    @Inject
    ChatConnectionsMap chatConnectionsMap;
    @Inject
    UserConnectionMap userConnectionMap;

    // return true if everything was added to Redis correctly. It stores:
    // 1. connectionId: userdata
    // 2. username: connectionId
    // 3. (for each chat of the user) chatId: add connectionId to the set
    public Uni<Boolean> storeConnection(String connectionId, String username) {
        return userRepository.getChatIds(username)
                .flatMap(chatIds -> {

                    // 1. Map the connectionId to the userdata
                    return connectionUserMap.storeConnection(connectionId, username, chatIds)
                            .flatMap(valuesStored -> {
                                if (valuesStored > 0 && chatIds != null && !chatIds.isEmpty()) {

                                    // 2. Map the username to the connectionId
                                    return userConnectionMap.storeUserConnection(username, connectionId)
                                            .flatMap(ignored -> {

                                                // For each chatId, add the current connectionId to the available ones for that chat
                                                return Multi.createFrom().iterable(chatIds)
                                                        .onItem().transformToUni(chatId ->
                                                                chatConnectionsMap.storeAvailableConnectionForChat(chatId, connectionId)
                                                        ).concatenate().collect().asList().replaceWith(true);
                                            });
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

    public Uni<Void> removeConnection(String connectionId) {

        // Get the username associated with that connectionId
        return connectionUserMap.getValueFromConnection(connectionId, "username")
                .onItem().ifNotNull().transformToUni(username -> {

                    // Remove the connection from the ConnectionUserMap
                    return connectionUserMap.removeConnection(connectionId)
                            .flatMap(elemetsRemoved -> {
                                if (elemetsRemoved <= 0) {
                                    return Uni.createFrom().voidItem();
                                }

                                // If connection removed from ConnectionUserMap, remove the username from UserConnectionMap
                                return userConnectionMap.removeUserConnection(username)
                                        .replaceWithVoid();
                            });
                });
    }

    public Uni<String> getValueFromConnection(String connectionId, String value) {
        return connectionUserMap.getValueFromConnection(connectionId, value);
    }

    public Uni<Set<String>> getAvailableConnectionsForChat(ObjectId chatId) {
        return chatConnectionsMap.getAvailableConnectionsForChat(chatId);
    }

    public Uni<Integer> removerConnectionFromChat(ObjectId chatId, String connectionId) {
        return chatConnectionsMap.removerConnectionFromChat(chatId, connectionId);
    }
}
