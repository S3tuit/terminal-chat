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
    ConnectionUserMap connectionUserMap;
    @Inject
    ChatUsersMap chatUsersMap;
    @Inject
    UserConnectionMap userConnectionMap;


    // return true if everything was added to Redis correctly. It stores:
    // 1. connectionId: userdata
    // 2. username: connectionId
    // 3. (for each chat of the user) chatId: add connectionId to the set
    public Uni<Boolean> storeConnection(String connectionId, String username, Set<ObjectId> chatIds) {
        // 1. Map the connectionId to the userdata
        return Uni.createFrom().completionStage(connectionUserMap.storeConnection(connectionId, username, chatIds))
                .flatMap(valuesStored -> {
                    if (valuesStored > 0 && chatIds != null) {

                        // 2. Map the username to the connectionId
                        return Uni.createFrom().completionStage(userConnectionMap.storeUserConnection(username, connectionId))
                                .flatMap(ignored -> {

                                    // For each chatId, add the current connectionId to the available ones for that chat
                                    return Multi.createFrom().iterable(chatIds)
                                            .onItem().transformToUni(chatId ->
                                                    Uni.createFrom().completionStage(
                                                            chatUsersMap.storeAvailableUsernameForChat(chatId, username)
                                                    )
                                            )
                                            .merge().collect().asList()
                                            .replaceWith(true);
                                });
                    } else {
                        return Uni.createFrom().item(false);
                    }
                }).onFailure().recoverWithItem(th -> {
                    th.printStackTrace();
                    return false;
                });
    }

    public Uni<Void> removeConnection(String connectionId, String username, Set<ObjectId> chatIds) {

        // 1. Remove the username from the current online usernames for each chat they have access to
        return Multi.createFrom().iterable(chatIds)
                .onItem().transformToUni(chatId ->
                        Uni.createFrom().completionStage(
                                chatUsersMap.removerUsernameFromChat(chatId, username)
                        )
                )
                .merge().collect().asList() // Wait for all chat removals to complete
                .replaceWithVoid()
                .flatMap(ignored ->
                        // 2. Remove the connection from the ConnectionUserMap
                        Uni.createFrom().completionStage(connectionUserMap.removeConnection(connectionId))
                )
                .flatMap(elementsRemoved -> {
                    // 3. Remove the username from UserConnectionMap
                    return Uni.createFrom().completionStage(userConnectionMap.removeUserConnection(username))
                            .replaceWithVoid();
                });
    }

    public Uni<String> getValueFromConnection(String connectionId, String value) {
        return Uni.createFrom().completionStage(connectionUserMap.getValueFromConnection(connectionId, value));
    }

    public Uni<Set<String>> getAvailableUsernamesForChat(ObjectId chatId) {
        return Uni.createFrom().completionStage(chatUsersMap.getAvailableUsernamesForChat(chatId));
    }

    public Uni<Long> removerUsernameFromChat(ObjectId chatId, String username) {
        return Uni.createFrom().completionStage(chatUsersMap.removerUsernameFromChat(chatId, username));
    }
}
