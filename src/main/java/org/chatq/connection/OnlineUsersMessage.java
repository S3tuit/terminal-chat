package org.chatq.connection;

import org.bson.types.ObjectId;

import java.util.Set;
import java.util.stream.Collectors;

public class OnlineUsersMessage {

    private Set<String> usernames;
    private ObjectId chatId;

    public OnlineUsersMessage(Set<String> usernames, ObjectId chatId) {
        this.usernames = usernames;
        this.chatId = chatId;
    }

    public Set<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(Set<String> usernames) {
        this.usernames = usernames;
    }

    public ObjectId getChatId() {
        return chatId;
    }

    public void setChatId(ObjectId chatId) {
        this.chatId = chatId;
    }

    public String toJson() {
        String usernamesJson = usernames.stream()
                .map(username -> "\"" + username + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        String chatIdJson = chatId != null ? "\"" + chatId.toHexString() + "\"" : "null";

        return String.format("{ \"type\": \"OnlineUsersMessage\", \"usernames\": %s, \"chatId\": %s}", usernamesJson, chatIdJson);
    }
}
