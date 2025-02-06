package org.chatq.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.chatq.users.User;

@ApplicationScoped
public class AuthService {

    @Inject
    GenerateToken generateToken;

    public String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password);
    }

    // Returns a token if the username and password are present at db
    public TokenResponse validateLogin(String username, String plainPassword) {
        User user = User.find("{ username: ?1 }", username).firstResult();
        if (user != null && BcryptUtil.matches(plainPassword, user.hashedPassword)) {
            return generateToken.generateToken(user.username);
        } else {
            return null;
        }
    }

}
