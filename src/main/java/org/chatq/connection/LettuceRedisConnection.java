package org.chatq.connection;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LettuceRedisConnection {

    // This gives problems with Quarkus CDI, so it uses lazy initialization

    private RedisAsyncCommands<String, String> redisCommandsInstance;

    private final String redisHost;

    public LettuceRedisConnection(@ConfigProperty(name = "REDIS_HOST") String redisHost) {
        this.redisHost = redisHost;
    }

    // Getter for redisCommands with lazy initialization
    public RedisAsyncCommands<String, String> getRedisCommands() {
        if (redisCommandsInstance == null) { // Lazy initialization check
            RedisClient redisClient = RedisClient.create(redisHost);
            StatefulRedisConnection<String, String> connection = redisClient.connect();
            redisCommandsInstance = connection.async();
        }
        return redisCommandsInstance;
    }
}
