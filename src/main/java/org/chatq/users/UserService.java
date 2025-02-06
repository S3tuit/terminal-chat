package org.chatq.users;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.chatq.auth.AuthService;

@ApplicationScoped
public class UserService {

    @Inject
    AuthService authService;

    public boolean addUser(String username, String plainPassword) {
        User user = new User(username, authService.hashPassword(plainPassword), null);
        user.persist();
        return true;
    }

    public boolean hasAccessToChat(String username, ObjectId chatId) {
        return User.hasChat(username, chatId);
    }
}
