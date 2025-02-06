package org.chatq.chat;

import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class ChatMessageService {

    public List<ChatMessage> getChatMessages(ObjectId chatId, int page) {
        ObjectId chatIdObj = Chat.getChatIdIfExists(chatId);
        if (chatIdObj == null) {
            return null;
        }

        return ChatMessage.getChatMessagesPage(chatIdObj, page);
    }
}
