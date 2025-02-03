package org.chatq.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.chatq.entities.User;

@ApplicationScoped
public class AuthService {


    public static String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password);
    }

    public static User validateLogin(String username, String password) {
        User user = User.find("{ username: ?1 }", username).firstResult();
        if (user != null && BcryptUtil.matches(password, user.hashedPassword)) {
            return user;
        } else {
            return null;
        }
    }

    public static User validateLogin(User user) {
        if (user == null || user.username == null || user.hashedPassword == null) {
            return null;
        } else {
            return validateLogin(user.hashedPassword, user.username);
        }
    }
}
