package org.chatq.users;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.chatq.auth.AuthService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


@ApplicationScoped
public class UserRepository implements ReactivePanacheMongoRepository<User> {

    @Inject
    AuthService authService;

    public Uni<User> addUser(String username, String plainPassword) {
        User user = new User(username, authService.hashPassword(plainPassword));
        return persist(user);
    }

    public Uni<Boolean> hasChat(String username, ObjectId chatId) {
        if (chatId == null || username == null) {
            return Uni.createFrom().item(false);
        }
        return find("{ 'username': ?1, 'chatIds': ?2 }", username, chatId)
                .firstResult()
                .onItem().transform(user -> user != null);
    }

    public Uni<User> getUserFromUsername(String username) {
        return find("{ 'username': ?1 }", username)
                .firstResult();
    }

    public Uni<Set<ObjectId>> getChatIds(String username) {
        return find("{ 'username': ?1 }", username)
                .firstResult()
                .onItem().ifNotNull().transform(user -> {
                    if (user != null && user.chatIds != null && !user.chatIds.isEmpty()) {
                        return user.chatIds;
                    }
                    return new HashSet<ObjectId>();
                })
                .onItem().ifNull().continueWith(Collections.emptySet());

    }

    // returns true if the chatId was added, false if it was already present or user not found
    public Uni<User> addChatIdToUser(ObjectId userId, ObjectId chatId) {
        return findById(userId)
                .onItem().ifNotNull().transformToUni(user -> {
                    if (user == null) {
                        return null;
                    }

                    if (user.chatIds == null) {
                        user.chatIds = new HashSet<>();
                    }

                    if (user.chatIds.add(chatId)) {
                        return update(user);
                    }
                    return null;
                });
    }
}
