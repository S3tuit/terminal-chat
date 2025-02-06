package org.chatq.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;
import org.chatq.entities.Chat;
import org.chatq.entities.ChatMessage;

import java.util.List;

@ApplicationScoped
public class ChatMessageService {

    public List<ChatMessage> getChatMessages(String chatId, int page) {
        ObjectId chatIdObj = Chat.getChatIdIfExists(chatId);
        if (chatIdObj == null) {
            return null;
        }

        return ChatMessage.getChatMessagesPage(chatIdObj, page);
    }

    public List<ChatMessage> getChatMessages(ObjectId chatId, int page) {
        ObjectId chatIdObj = Chat.getChatIdIfExists(chatId);
        if (chatIdObj == null) {
            return null;
        }

        return ChatMessage.getChatMessagesPage(chatIdObj, page);
    }
}
