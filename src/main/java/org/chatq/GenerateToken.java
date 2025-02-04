package org.chatq;

import java.util.Arrays;
import java.util.HashSet;


import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.Claims;

import io.smallrye.jwt.build.Jwt;

@Path("/auth")
public class GenerateToken {

    @POST
    @Path("/generate-token")
    @Consumes("application/json")
    @Produces("application/json")
    public Response generateToken(UserRequest userRequest) {
        if (userRequest.getUsername() == null || userRequest.getUsername().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Username cannot be null or empty.")
                    .build();
        }

        String token = Jwt.issuer("chatq-auth-service") // Set the issuer
                .upn(userRequest.getUsername()) // Use the provided username
                .groups(new HashSet<>(Arrays.asList("User", "Admin"))) // Set default groups
                .claim(Claims.birthdate.name(), "2001-07-13") // Optional custom claim
                .sign(); // Sign the JWT with the private key

        return Response.ok(new TokenResponse(token)).build();
    }

    // Inner class for user input
    public static class UserRequest {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    // Inner class for token response
    public static class TokenResponse {
        private String token;

        public TokenResponse(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
