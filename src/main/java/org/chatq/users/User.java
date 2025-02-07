package org.chatq.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.HashSet;
import java.util.Set;

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

    public static Set<ObjectId> getChatIds(String username) {
        User user = User.find("{ 'username': ?1 }", username).firstResult();
        return user != null ? user.chatIds : null;
    }

    // returns true if the chatId was added, false if it was already present or user not found
    public static boolean addChatIdToUser(ObjectId userId, ObjectId chatId) {
        User user = User.findById(userId);
        if (user == null) {
            return false;
        }

        boolean added = user.chatIds.add(chatId);

        if (added) {
            user.update();
        }
        return added;
    }
}
