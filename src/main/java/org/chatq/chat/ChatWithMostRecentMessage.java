package org.chatq.chat;

import io.quarkus.mongodb.panache.common.MongoEntity;

// DTO to include the mostRecentMessage in the Chat entity
@MongoEntity(collection = "Chat")
public class ChatWithMostRecentMessage extends Chat {

    public ChatMessage mostRecentMessage;
}
