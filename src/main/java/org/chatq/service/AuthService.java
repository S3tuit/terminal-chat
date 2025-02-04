package org.chatq.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.chatq.entities.TempUser;
import org.chatq.entities.User;

@ApplicationScoped
public class AuthService {


    public static String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password);
    }

    public static User validateLogin(String username, String plainPassword) {
        User user = User.find("{ username: ?1 }", username).firstResult();
        if (user != null && BcryptUtil.matches(plainPassword, user.hashedPassword)) {
            return user;
        } else {
            return null;
        }
    }

    public static User validateLogin(TempUser tempUser) {
        if (tempUser == null || tempUser.username == null || tempUser.plainPassword == null) {
            return null;
        } else {
            return validateLogin(tempUser.plainPassword, tempUser.username);
        }
    }
}
