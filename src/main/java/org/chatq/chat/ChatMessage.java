package org.chatq.chat;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;

@MongoEntity
public class ChatMessage extends PanacheMongoEntity {

    public String fromUsername;
    public String message;
    public Instant timestamp;
    public ObjectId chatId;


    public ChatMessage() {}

    public ChatMessage(String fromUsername, String message, Instant timestamp, Chat chat) {
        this.fromUsername = fromUsername;
        this.message = message;
        this.timestamp = timestamp;
        this.chatId = chat.id;
    }

    public ChatMessage(String fromUsername, String message, Instant timestamp, ObjectId chatId) {
        this.fromUsername = fromUsername;
        this.message = message;
        this.timestamp = timestamp;
        this.chatId = chatId;
    }

    // Return a page of 10 (max) ChatMessages ordered by the latest sent
    public static List<ChatMessage> getChatMessagesPage(ObjectId chatId, int page) {
        PanacheQuery<ChatMessage> messages = ChatMessage.
                find("{ chatId: ?1 }", Sort.by("timestamp", Sort.Direction.Descending), chatId);

        return messages.page(Page.of(page, 10)).list();
    }

    public static List<ChatMessage> getChatMessagesPage(ObjectId chatId) {
        return ChatMessage.getChatMessagesPage(chatId, 0);
    }

    // Since the ObjectMapper doesn't work well with ObjectId and Instant...
    // return a valid json excluding the chatId value
    public String toJsonNoChatId() {

        return String.format(
                "{ \"fromUsername\": \"%s\", \"message\": \"%s\", \"timestamp\": \"%s\" }",
                this.fromUsername,
                this.message,
                this.timestamp.toString() // Use ISO-8601 format for timestamp
        );
    }
}
