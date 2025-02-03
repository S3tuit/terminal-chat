package org.chatq.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.chatq.entities.User;

@ApplicationScoped
public class UserService {

    public boolean addUser(String username, String plainPassword) {
        User user = new User(username, AuthService.hashPassword(plainPassword), null);
        user.persist();
        return true;
    }
}
