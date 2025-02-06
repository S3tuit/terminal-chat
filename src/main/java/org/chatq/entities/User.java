package org.chatq.entities;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.Set;

@MongoEntity
public class User extends PanacheMongoEntity {

    public String username;
    public String hashedPassword;
    public Set<ObjectId> chatIds;

    public User() {}

    public User(String username, String hashedPassword, Set<ObjectId> chatIds) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.chatIds = chatIds;
    }

    public User(String username, String hashedPassword) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.chatIds = null;
    }

    public static boolean userHasChat(String username, ObjectId chatId) {
        if (chatId == null || username == null) {
            return false;
        }
        return User.find("{ 'username': ?1, 'chatIds': ?2 }", username, chatId).firstResult() != null;
    }
}
