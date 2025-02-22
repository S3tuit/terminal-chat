package org.chatq.chat;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.chatq.users.UserRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

@ApplicationScoped
public class ChatRepository implements ReactivePanacheMongoRepository<Chat> {

    @Inject
    UserRepository userRepository;

    public Uni<Chat> createChat(Boolean direct, String chatName, String createdBy) {
        try {
            ObjectId createdByObjId = new ObjectId(createdBy);
            return createChat(direct, chatName, createdByObjId);
        } catch (Exception e) {
            e.printStackTrace();
            return Uni.createFrom().nullItem();
        }
    }

    // Create a new chat and assign it to the available chats for the user who created it
    public Uni<Chat> createChat(Boolean direct, String chatName, ObjectId createdBy) {
        Chat chat = new Chat(direct, chatName, createdBy, Instant.now(), new HashSet<>(Arrays.asList(createdBy)));

        return persist(chat)
                .flatMap(newChat -> {
                    if (newChat == null) {
                        return Uni.createFrom().nullItem();
                    }
                    return userRepository.addChatIdToUser(newChat.createdBy, newChat.id);
                })
                .replaceWith(chat)
                .onFailure().invoke(e -> {
                    System.err.println("Error creating chat: " + e.getMessage());
                });
    }

    public Uni<Boolean> addUserToChat(ObjectId userId, ObjectId chatId) {
        return findById(chatId)
                .onItem().ifNotNull().transformToUni(chat -> {
                    boolean added = chat.userIds.add(userId);
                    if (added) {
                        // persist changes
                        return chat.update().replaceWith(true);
                    } else {
                        return Uni.createFrom().item(false);
                    }
                }).onItem().ifNull().continueWith(false);
    }

    // Returns a Uni that emits the chat id if the chat exists
    public Uni<ObjectId> getChatIdIfExists(ObjectId id) {
        return findById(id)
                .onItem().transform(chat -> chat != null ? chat.id : null);
    }

    public Uni<ObjectId> getChatIdIfExists(String chatId) {
        try {
            ObjectId objectId = new ObjectId(chatId);
            return getChatIdIfExists(objectId);
        } catch (Exception e) {
            e.printStackTrace();
            return Uni.createFrom().nullItem();
        }
    }
}
