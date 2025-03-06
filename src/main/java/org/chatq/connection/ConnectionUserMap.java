package org.chatq.connection;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.Map;
import java.util.Set;

// Map the connectionId with the username and the chatIds the user has access to

@ApplicationScoped
public class ConnectionUserMap {

    private final RedisAsyncCommands<String, String> redisCommands;


    public ConnectionUserMap(LettuceRedisConnection lettuceRedisConnection) {
        this.redisCommands = lettuceRedisConnection.getRedisCommands();
    }

    // Store a connection, return a Long indicating the number of fields that were added to the hash
    public RedisFuture<Long> storeConnection(String connectionId, String username, Set<ObjectId> chatIds) {
        String key = "connection:" + connectionId;
        return redisCommands.hset(key, Map.of(
                "username", username,
                "chatIds", chatIds.toString()
        ));
    }

    // Retrieve a connection
    public RedisFuture<Map<String, String>> getUserdata(String connectionId) {
        String key = "connection:" + connectionId;
        return redisCommands.hgetall(key);
    }

    // Retrieve a value from a connection
    public RedisFuture<String> getValueFromConnection(String connectionId, String value) {
        String key = "connection:" + connectionId;
        return redisCommands.hget(key, value);
    }

    // Delete a connection
    public RedisFuture<Long> removeConnection(String connectionId) {
        String key = "connection:" + connectionId;
        return redisCommands.hdel(key, new String[]{"username", "chatIds"});
    }
}
