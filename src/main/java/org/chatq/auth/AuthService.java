package org.chatq.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.chatq.users.User;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class AuthService {

    @Inject
    GenerateToken generateToken;
    @Inject
    JWTParser jwtParser;

    public String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password);
    }

    // Returns a token if the username and password are present at db
    public TokenResponse validateLogin(String username, String plainPassword) {
        User user = User.find("{ username: ?1 }", username).firstResult();
        if (user != null && BcryptUtil.matches(plainPassword, user.hashedPassword)) {
            return generateToken.generateToken(user.username, user.id);
        } else {
            return null;
        }
    }

    // return the username associated to the token if it's valid and if the user has access to that chat
    public String getUsernameIfPermission(String token, ObjectId chatId) {
        try {
            JsonWebToken jwt = jwtParser.parse(token);
            if (
                    jwt.getClaim("upn") != null
                    && jwt.getGroups().contains("User")
                    && User.hasChat(jwt.getClaim("upn"), chatId)
            ) {
                return jwt.getClaim("upn").toString();
            }
        } catch (ParseException ex) {
            // TO CHANGE: this logs the JWT claims as plain text
            ex.printStackTrace();
        }

        return null;
    }

}
