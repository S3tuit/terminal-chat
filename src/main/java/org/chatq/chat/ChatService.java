package org.chatq.chat;

import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.Instant;

@ApplicationScoped
public class ChatService {

    public boolean createChat(Boolean direct, String chatName, ObjectId createdBy) {
        try {
            Chat chat = new Chat(direct, chatName, createdBy, Instant.now());
            chat.persist();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Chat createChat(Boolean direct, String chatName, String createdBy) {
        try {
            ObjectId createdByObjId = new ObjectId(createdBy);
            Chat chat = new Chat(direct, chatName, createdByObjId, Instant.now());
            chat.persist();
            return chat;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
