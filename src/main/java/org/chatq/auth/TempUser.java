package org.chatq.auth;


// Not a mongo entity. Used to store info of a user who's not authorized yet
public class TempUser {

    private String username;
    private String plainPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPlainPassword() {
        return plainPassword;
    }

    public void setPlainPassword(String plainPassword) {
        this.plainPassword = plainPassword;
    }

    public TempUser() {}

    public TempUser(String username, String hashedPassword) {
        this.username = username;
        this.plainPassword = hashedPassword;
    }

}
