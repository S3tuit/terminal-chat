package org.chatq.chat;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Set;

@MongoEntity
public class Chat extends ReactivePanacheMongoEntity {

    // Whether the chat is a direct (one to one)
    public Boolean direct;
    public String chatName;
    public ObjectId createdBy;
    public Instant createdAt;
    public Set<ObjectId> userIds;


    public Chat () {};

    public Chat (Boolean direct, String chatName, ObjectId createdBy, Instant createdAt, Set<ObjectId> userIds) {
        this.direct = direct;
        this.chatName = chatName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.userIds = userIds;
    }

}
