package org.chatq.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.chatq.entities.TempUser;
import org.chatq.entities.User;

@ApplicationScoped
public class AuthService {

    @Inject
    GenerateToken generateToken;

    public String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password);
    }

    public TokenResponse validateLogin(String username, String plainPassword) {
        User user = User.find("{ username: ?1 }", username).firstResult();
        if (user != null && BcryptUtil.matches(plainPassword, user.hashedPassword)) {
            return generateToken.generateToken(user.username);
        } else {
            return null;
        }
    }

    public TokenResponse validateLogin(TempUser tempUser) {
        if (tempUser == null || tempUser.username == null || tempUser.plainPassword == null) {
            return null;
        } else {
            return validateLogin(tempUser.plainPassword, tempUser.username);
        }
    }
}
