package org.chatq.connection;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import jakarta.enterprise.context.ApplicationScoped;


// Map the username to his current connectionId

@ApplicationScoped
public class UserConnectionMap {

    private final RedisAsyncCommands<String, String> redisCommands;


    public UserConnectionMap(LettuceRedisConnection lettuceRedisConnection) {
        this.redisCommands = lettuceRedisConnection.getRedisCommands();
    }

    public RedisFuture<String> storeUserConnection(String username, String connectionId) {
        return redisCommands.set(username, connectionId);
    }

    public RedisFuture<String> getConnectionFromUser(String username) {
        return redisCommands.get(username);
    }

    public RedisFuture<String> removeUserConnection(String username) {
        return redisCommands.getdel(username);
    }

    public RedisFuture<String> updateUserConnection(String username, String newConnectionId) {
        return redisCommands.set(username, newConnectionId);
    }
}
