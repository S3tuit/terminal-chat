package org.chatq.entities;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.List;

@MongoEntity
public class User extends PanacheMongoEntity {

    public String username;
    public String hashedPassword;
    public List<ObjectId> chatIds;

    public User() {}

    public User(String username, String hashedPassword, List<ObjectId> chatIds) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.chatIds = chatIds;
    }

    public User(String username, String hashedPassword) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.chatIds = null;
    }
}
