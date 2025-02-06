package org.chatq.auth;

// Not a mongo entity. It's used to parse the token as a json {"token": "actual_token"} in the responses
public class TokenResponse {

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
