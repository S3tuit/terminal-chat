package org.chatq.entities;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

import java.time.Instant;

@MongoEntity
public class ChatMessage extends PanacheMongoEntity {

    public String fromUsername;
    public String message;
    public Instant timestamp;

}
