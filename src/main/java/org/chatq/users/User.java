package org.chatq.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.chatq.chat.ChatWithMostRecentMessage;

import java.util.*;

@MongoEntity
public class User extends PanacheMongoEntity {

    public String username;
    public String hashedPassword;
    public Set<ObjectId> chatIds = new HashSet<>();

    public User() {}

    public User(String username, String hashedPassword, Set<ObjectId> chatIds) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.chatIds = chatIds;
    }

    public User(String username, String hashedPassword) {
        this.username = username;
        this.hashedPassword = hashedPassword;
    }

    // Return a JSON representation of the user
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Enable pretty printing for better readability (optional)
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            // Convert the User object to JSON
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}"; // Return an empty JSON object in case of an error
        }
    }

    public static boolean hasChat(String username, ObjectId chatId) {
        if (chatId == null || username == null) {
            return false;
        }
        return User.find("{ 'username': ?1, 'chatIds': ?2 }", username, chatId).firstResult() != null;
    }

    public static List<PanacheMongoEntityBase> getChats(String username) {
        User user = User.find("{ 'username': ?1 }", username).firstResult();

        if (user != null && user.chatIds != null && !user.chatIds.isEmpty()) {
            // ChatWithMostRecentMessage is a DTO of the Chat entity
            return ChatWithMostRecentMessage.mongoCollection()
                    .aggregate(Arrays.asList(
                            // Filter for the chats user has access to
                            Aggregates.match(Filters.in("_id", user.chatIds)),
                            // Match all the ChatMessages with that chatId
                            Aggregates.lookup("ChatMessage", "_id", "chatId", "messages"),
                            // Get just the most recent message
                            Aggregates.addFields(
                                    new Field<>("mostRecentMessage",
                                            new Document("$arrayElemAt", Arrays.asList(
                                                            new Document("$sortArray",
                                                                    new Document("input", "$messages")
                                                                            .append("sortBy", new Document("timestamp", -1))),
                                                            0)))
                            ),
                            // Exclude the messages nested document created in the lookup
                            Aggregates.project(new Document("messages", 0))
                    ))
                    .into(new ArrayList<>());
        }

        return Collections.emptyList();
    }

    // returns true if the chatId was added, false if it was already present or user not found
    public static boolean addChatIdToUser(ObjectId userId, ObjectId chatId) {
        User user = User.findById(userId);
        if (user == null) {
            return false;
        }

        if (user.chatIds == null) {
            user.chatIds = new HashSet<>();
        }
        boolean added = user.chatIds.add(chatId);

        if (added) {
            user.update();
        }
        return added;
    }
}
