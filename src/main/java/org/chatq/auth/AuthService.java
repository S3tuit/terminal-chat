package org.chatq.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.chatq.users.User;
import org.chatq.users.UserRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class AuthService {

    @Inject
    GenerateToken generateToken;
    @Inject
    JWTParser jwtParser;
    @Inject
    UserRepository userRepository;

    public String hashPassword(String password) {
        return BcryptUtil.bcryptHash(password);
    }

    // Returns a token if the username and password are present at db
    public Uni<TokenResponse> validateLogin(String username, String plainPassword) {
        return userRepository.find("{ username: ?1 }", username)
                .firstResult()
                .onItem().ifNotNull().transform(user -> {
                    if (BcryptUtil.matches(plainPassword, user.hashedPassword)) {
                        return generateToken.generateToken(user.username, user.id);
                    }
                    return null;
                })
                .onItem().ifNull().continueWith(() -> null);
    }

    // return the username associated to the token if it's valid and if the user has the role User
    public String getUsernameIfPermission(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            JsonWebToken jwt = jwtParser.parse(token);
            if (
                    jwt.getClaim("upn") != null
                            && jwt.getGroups().contains("User")
            ) {
                return jwt.getClaim("upn").toString();
            }
        } catch (ParseException ex) {
            // TO CHANGE: this logs the JWT claims as plain text
            ex.printStackTrace();
        }

        return null;
    }

    public static String getClaimFromCtx(SecurityContext ctx, String claimName) {
        // Check for token validity
        if (ctx.getUserPrincipal() == null) {
            return null;
        }
        JsonWebToken jwt = (JsonWebToken) ctx.getUserPrincipal();
        if (jwt.getClaim(claimName) == null) {
            return null;
        }
        return jwt.getClaim(claimName).toString();
    }
}
