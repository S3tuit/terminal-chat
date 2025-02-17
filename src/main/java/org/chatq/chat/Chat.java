package org.chatq.chat;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Set;

@MongoEntity
public class Chat extends PanacheMongoEntity {

    // Whether the chat is a direct (one to one)
    public Boolean direct;
    public String chatName;
    public ObjectId createdBy;
    public Instant createdAt;
    public Set<ObjectId> userIds;


    public Chat () {};

    public Chat (Boolean direct, String chatName, ObjectId createdBy, Instant createdAt, Set<ObjectId> userIds) {
        this.direct = direct;
        this.chatName = chatName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.userIds = userIds;
    }

    // returns true if the userId was added, false if it was already present or else
    public static boolean addUserToChat (ObjectId userId, ObjectId chatId) {
        Chat chat = Chat.findById(chatId);
        if (chat == null) {
            return false;
        }

        boolean added = chat.userIds.add(userId);
        if (added) {
            chat.update();
        }
        return added;
    }

    public static ObjectId getChatIdIfExists(ObjectId id) {
        Chat chat = findById(id);
        return chat != null ? chat.id : null;
    }

    public static ObjectId getChatIdIfExists(String chatId) {
        try {
            return getChatIdIfExists(new ObjectId(chatId));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
