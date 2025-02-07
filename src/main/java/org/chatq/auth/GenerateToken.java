package org.chatq.auth;

import java.util.HashSet;
import java.util.List;


import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.jwt.build.Jwt;
import org.bson.types.ObjectId;

@ApplicationScoped
public class GenerateToken {

    public TokenResponse generateToken(String username, ObjectId userId) {
        if (username == null || username.isEmpty()) {
            return null;
        }

        String token = Jwt.issuer("chatq-auth-service")
                .upn(username) // UserPrincipal = username
                .groups(new HashSet<>(List.of("User"))) // Assign role
                .claim("userId", userId.toString())
                .sign();

        return new TokenResponse(token);
    }


}
