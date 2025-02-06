package org.chatq.auth;

import java.util.HashSet;
import java.util.List;


import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.jwt.build.Jwt;

@ApplicationScoped
public class GenerateToken {

    public TokenResponse generateToken(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }

        String token = Jwt.issuer("chatq-auth-service")
                .upn(username) // UserPrincipal = username
                .groups(new HashSet<>(List.of("User"))) // Assign role
                .sign();

        return new TokenResponse(token);
    }


}
