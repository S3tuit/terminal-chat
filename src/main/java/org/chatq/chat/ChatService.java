package org.chatq.chat;

import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;
import org.chatq.users.User;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

@ApplicationScoped
public class ChatService {

    public boolean createChat(Boolean direct, String chatName, ObjectId createdBy) {
        try {
            Chat chat = new Chat(direct, chatName, createdBy, Instant.now(), new HashSet<>(Arrays.asList(createdBy)));
            chat.persist();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createChat(Boolean direct, String chatName, String createdBy) {
        try {
            ObjectId createdByObjId = new ObjectId(createdBy);
            Chat chat = new Chat(direct, chatName, createdByObjId, Instant.now(), new HashSet<>(Arrays.asList(createdByObjId)));
            chat.persist();

            return User.addChatIdToUser(createdByObjId, chat.id);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
