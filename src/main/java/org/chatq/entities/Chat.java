package org.chatq.entities;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

@MongoEntity
public class Chat extends PanacheMongoEntity {

    // Whether the chat is a direct (one to one)
    public boolean direct;

    public static Chat findChatById(String id) {
        ObjectId chatId = new ObjectId(id);
        return findById(chatId);
    }

    public static Chat findChatById(ObjectId id) {
        return findById(id);
    }

    public static ObjectId getChatIdIfExists(String id) {
        Chat chat = findChatById(id);
        return chat != null ? chat.id : null;
    }

    public static ObjectId getChatIdIfExists(ObjectId id) {
        Chat chat = findChatById(id);
        return chat != null ? chat.id : null;
    }
}
