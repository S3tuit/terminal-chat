package org.chatq.connection;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.Set;

// Maps the chatId with the current available connections that have access to that chat, chatId: Set<connectionId>

@ApplicationScoped
public class ChatConnectionsMap {

    private final ReactiveSetCommands<String, String> setCommands;

    public ChatConnectionsMap(ReactiveRedisDataSource reactive) {
        this.setCommands = reactive.set(String.class);
    }

    // Add the connection to the available connections for that chatId
    public Uni<Integer> storeAvailableConnectionForChat(ObjectId chatId, String connectionId) {
        return setCommands.sadd("chat:" + chatId.toString(),
                connectionId);
    }

    // Retrieve the available connections for that chat
    public Uni<Set<String>> getAvailableConnectionsForChat(ObjectId chatId) {
        return setCommands.smembers("chat:" + chatId.toString());
    }

    public Uni<Integer> removerConnectionFromChat(ObjectId chatId, String connectionId) {
        return setCommands.srem("chat:" + chatId.toString(),
                connectionId);
    }
}
