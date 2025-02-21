package org.chatq.chat;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.Set;

@ApplicationScoped
public class ChatSessionRepository {

    private final ReactiveSetCommands<String, String> setCommands;

    public ChatSessionRepository(ReactiveRedisDataSource reactive) {
        this.setCommands = reactive.set(String.class);
    }

    // Add the session to the available sessions for that chatId
    public Uni<Integer> storeAvailableSessionForChat(ObjectId chatId, String sessionId) {
        return setCommands.sadd("chat:" + chatId.toString(),
                sessionId);
    }

    // Retrieve the available sessions for that chat
    public Uni<Set<String>> getAvailableSessionsForChat(ObjectId chatId) {
        return setCommands.smembers("chat:" + chatId.toString());
    }

    public Uni<Integer> removerSessionFromChat(ObjectId chatId, String sessionId) {
        return setCommands.srem("chat:" + chatId.toString(),
                sessionId);
    }
}
