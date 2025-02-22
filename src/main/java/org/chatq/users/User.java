package org.chatq.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import org.bson.types.ObjectId;

import java.util.*;

@MongoEntity
public class User extends ReactivePanacheMongoEntity {

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

}
