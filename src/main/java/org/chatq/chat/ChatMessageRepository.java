package org.chatq.chat;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.chatq.users.User;
import org.chatq.users.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class ChatMessageRepository implements ReactivePanacheMongoRepository<ChatMessage> {

    @Inject
    UserRepository userRepository;

    // Return a page of 10 (max) ChatMessages ordered by the latest sent
    public Uni<List<ChatMessage>> getChatMessagesPage(ObjectId chatId, int page) {
        ReactivePanacheQuery<ChatMessage> messages = find("{ chatId: ?1 }",
                Sort.by("timestamp", Sort.Direction.Descending), chatId);

        return messages.page(Page.of(page, 10)).list();
    }

    public Uni<List<ChatMessage>> getChatMessagesPage(ObjectId chatId) {
        return this.getChatMessagesPage(chatId, 0);
    }

    public Uni<List<ReactivePanacheMongoEntityBase>> getChatsAndLatestMsg(String username) {
        return userRepository.getUserFromUsername(username)
                .onItem().ifNotNull().transformToUni(user -> {
                    if (user.chatIds == null || user.chatIds.isEmpty()) {
                        return Uni.createFrom().item(new ArrayList<ReactivePanacheMongoEntityBase>());
                    }

                    // ChatWithMostRecentMessage is a DTO of the Chat entity
                   return ChatWithMostRecentMessage.mongoCollection()
                           .aggregate(Arrays.asList(
                                   // Filter for the chats the user has access to
                                   Aggregates.match(Filters.in("_id", user.chatIds)),
                                   // Match all the ChatMessages with that chatId
                                   Aggregates.lookup("ChatMessage", "_id", "chatId", "messages"),
                                   // Get just the most recent message
                                   Aggregates.addFields(
                                           new Field<>("mostRecentMessage",
                                                   new Document("$arrayElemAt", Arrays.asList(
                                                           new Document("$sortArray",
                                                                   new Document("input", "$messages")
                                                                           .append("sortBy", new Document("timestamp", -1))
                                                           ),
                                                           0))
                                           )
                                   ),
                                   // Exclude the messages nested document created in the lookup
                                   Aggregates.project(new Document("messages", 0))
                           ))
                           .collect().asList();
                })
                .onItem().ifNull().continueWith(() -> new ArrayList<ReactivePanacheMongoEntityBase>());
    }
}
