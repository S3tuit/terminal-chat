package org.chatq.chat;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;

@MongoEntity
public class ChatMessage extends ReactivePanacheMongoEntity {

    public String fromUsername;
    public String message;
    public Instant timestamp;
    public ObjectId chatId;


    public ChatMessage() {}

    public ChatMessage(String fromUsername, String message, String chatId) {
        this.fromUsername = fromUsername;
        this.message = message;
        this.timestamp = Instant.now();
        this.chatId = new ObjectId(chatId);
    }

    public ChatMessage(String fromUsername, String message, Instant timestamp, ObjectId chatId) {
        this.fromUsername = fromUsername;
        this.message = message;
        this.timestamp = timestamp;
        this.chatId = chatId;
    }

    public boolean messageValidity() {
        return fromUsername != null
                && message != null
                && timestamp != null
                && chatId != null;
    }

    // Used because the ObjectMapper doesn't work well with ObjectId and Instant...
    // this is a custom converter, returns a valid json. It includes a class field
    public String toJson() {

        return String.format(
                "{ \"type\": \"ChatMessage\", \"fromUsername\": \"%s\", \"message\": \"%s\", \"timestamp\": \"%s\", \"chatId\": \"%s\" }",
                this.fromUsername,
                this.message,
                this.timestamp.toString(), // Use ISO-8601 format for timestamp
                this.chatId.toString()
        );
    }
}
