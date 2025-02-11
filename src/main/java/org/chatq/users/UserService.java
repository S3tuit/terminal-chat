package org.chatq.users;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.chatq.auth.AuthService;
import org.chatq.chat.Chat;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class UserService {

    @Inject
    AuthService authService;

    public boolean addUser(String username, String plainPassword) {
        try {
            User user = new User(username, authService.hashPassword(plainPassword), null);
            user.persist();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasAccessToChat(String username, ObjectId chatId) {
        return User.hasChat(username, chatId);
    }

    public List<Chat> getUserChats(String username) {
        return User.getChats(username);
    }

    public boolean addChatToUser(String userId, ObjectId chatId) {
        if (userId == null || chatId == null) {
            throw new NullPointerException("userId and chatId cannot be null");
        }

        try{
            ObjectId userIdObj = new ObjectId(userId);
            return User.addChatIdToUser(userIdObj, chatId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
