package org.chatq.connection;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.Set;

// Maps the chatId with the current online usernames that have access to that chat, chatId: Set<username>

@ApplicationScoped
public class ChatUsersMap {


    private final RedisAsyncCommands<String, String> redisCommands;


    public ChatUsersMap(LettuceRedisConnection lettuceRedisConnection) {
        this.redisCommands = lettuceRedisConnection.getRedisCommands();
    }

    // Add the username to the available usernames for that chatId
    public RedisFuture<Long> storeAvailableUsernameForChat(ObjectId chatId, String username) {
        return redisCommands.sadd("chat:" + chatId.toString(), username);
    }

    // Retrieve the available usernames for that chat
    public RedisFuture<Set<String>> getAvailableUsernamesForChat(ObjectId chatId) {
        return redisCommands.smembers("chat:" + chatId.toString());
    }

    public RedisFuture<Long> removerUsernameFromChat(ObjectId chatId, String username) {
        return redisCommands.srem("chat:" + chatId.toString(), username);
    }
}
