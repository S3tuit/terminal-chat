package org.chatq.entities;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.List;

@MongoEntity
public class TempUser extends PanacheMongoEntity {

    public String username;
    public String plainPassword;

    public TempUser() {}

    public TempUser(String username, String hashedPassword) {
        this.username = username;
        this.plainPassword = hashedPassword;
    }

}
