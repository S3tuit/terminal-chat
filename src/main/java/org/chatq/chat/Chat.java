package org.chatq.chat;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;

@MongoEntity
public class Chat extends PanacheMongoEntity {

    // Whether the chat is a direct (one to one)
    public Boolean direct;
    public String chatName;
    public ObjectId createdBy;
    public Instant createdAt;


    public Chat () {};

    public Chat (Boolean direct, String chatName, ObjectId createdBy, Instant createdAt) {
        this.direct = direct;
        this.chatName = chatName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static Chat findChatById(ObjectId id) {
        return findById(id);
    }


    public static ObjectId getChatIdIfExists(ObjectId id) {
        Chat chat = findChatById(id);
        return chat != null ? chat.id : null;
    }
}
