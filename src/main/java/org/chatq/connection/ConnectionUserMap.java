package org.chatq.connection;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.Map;
import java.util.Set;

// Map the connectionId with the username and the chatIds the user has access to

@ApplicationScoped
public class ConnectionUserMap {

    private final ReactiveHashCommands<String, String, String> hashCommands;

    public ConnectionUserMap(ReactiveRedisDataSource reactive) {
        this.hashCommands = reactive.hash(String.class);
    }

    // Store a connection, return a Long indicating the number of fields that were added to the hash
    public Uni<Long> storeConnection(String connectionId, String username, Set<ObjectId> chatIds) {
        String key = "connection:" + connectionId;
        return hashCommands.hset(key, Map.of(
                "username", username,
                "chatIds", chatIds.toString()
        ));
    }

    // Retrieve a connection
    public Uni<Map<String, String>> getUserdata(String connectionId){
        String key = "connection:" + connectionId;
        return hashCommands.hgetall(key);
    }

    // Retrieve a value from a connection
    public Uni<String> getValueFromConnection(String connectionId, String value) {
        String key = "connection:" + connectionId;
        return hashCommands.hget(key, value);
    }

    // Delete a connection
    public Uni<Integer> removeConnection(String connectionId){
        String key = "connection:" + connectionId;
        return hashCommands.hdel(key, new String[]{"username", "chatIds"});
    }
}
