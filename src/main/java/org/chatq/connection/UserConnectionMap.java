package org.chatq.connection;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;


// Map the username to his current connectionId

@ApplicationScoped
public class UserConnectionMap {

    private final ReactiveValueCommands<String, String> valueCommands;

    public UserConnectionMap(ReactiveRedisDataSource reactive) {
        this.valueCommands = reactive.value(String.class);
    }

    public Uni<Void> storeUserConnection(String username, String connectionId) {
        return valueCommands.set(username, connectionId);
    }

    public Uni<String> getConnectionFromUser(String username) {
        return valueCommands.get(username);
    }

    public Uni<String> removeUserConnection(String username) {
        return valueCommands.getdel(username);
    }

    public Uni<Void> updateUserConnection(String username, String newConnectionId) {
        return valueCommands.set(username, newConnectionId);
    }
}
