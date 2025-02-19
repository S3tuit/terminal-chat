package org.chatq.chat;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class SessionService {

    private final ReactiveHashCommands<String, String, String> hashCommands;

    public SessionService(ReactiveRedisDataSource reactive) {
        this.hashCommands = reactive.hash(String.class);
    }

    // Store a session
    public Uni<Long> storeSession(String sessionId, String username, Set<ObjectId> chatIds){
        String key = "session:" + sessionId;
        return hashCommands.hset(key, Map.of(
                "username", username,
                "chatIds", chatIds.toString()
        ));
    }

    // Retrieve a session
    public Uni<Map<String, String>> getSession(String sessionId){
        String key = "session:" + sessionId;
        return hashCommands.hgetall(key);
    }

    // Retrieve a value
    public Uni<String> getValueFromSession(String sessionId, String value) {
        String key = "session:" + sessionId;
        return hashCommands.hget(key, value);
    }

    // Delete a session
    public Uni<Integer> removeSession(String sessionId){
        String key = "session:" + sessionId;
        return hashCommands.hdel(key);
    }
}
