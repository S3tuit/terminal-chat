package org.chatq.invite;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.chatq.chat.ChatRepository;
import org.chatq.users.UserRepository;

import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class InviteRepository implements ReactivePanacheMongoRepository<Invite> {

    @Inject
    ChatRepository chatRepository;
    @Inject
    UserRepository userRepository;

    public enum TimePeriod {

        MINUTES_30("30 minutes"),
        HOUR_1("1 hour"),
        HOUR_2("2 hour"),
        HOUR_8("8 hour"),
        DAY_1("1 day"),
        DAYS_7("7 days"),
        NEVER("never");

        private final String displayName;

        TimePeriod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Uni<Invite> createInvite(ObjectId chatId, InviteRepository.TimePeriod timePeriod) {
        Instant expiresAt = null;

        switch (timePeriod) {
            case MINUTES_30:
                expiresAt = Instant.now().plus(Duration.ofMinutes(30));
                break;
            case HOUR_1:
                expiresAt = Instant.now().plus(Duration.ofHours(1));
                break;
            case HOUR_2:
                expiresAt = Instant.now().plus(Duration.ofHours(2));
                break;
            case HOUR_8:
                expiresAt = Instant.now().plus(Duration.ofHours(8));
                break;
            case DAY_1:
                expiresAt = Instant.now().plus(Duration.ofDays(1));
                break;
            case DAYS_7:
                expiresAt = Instant.now().plus(Duration.ofDays(7));
                break;
            case NEVER: // No expiration
                break;
        }
        return this.createNewInvite(chatId, expiresAt);
    }

    public Uni<Invite> createNewInvite(ObjectId chatId, Instant expiresAt) {
        String inviteCode = Invite.generateRandomString(Invite.INVITE_LENGTH);
        Invite invite = new Invite(chatId, inviteCode, Instant.now(), expiresAt);
        return persist(invite)
                .onFailure().recoverWithItem(th -> {
                    return null;
                });
    }

    // Returns the chatId of the invite if it's a valid invite at db
    public Uni<ObjectId> getChatIdInsideInvite(String inviteCode) {
        return find("code", inviteCode).firstResult()
                .onItem().ifNotNull().transform(invite -> {
                    return invite.expiresAt.isBefore(Instant.now()) ? null : invite.chatId;
                })
                .onItem().ifNull().continueWith(() ->
                        null
        );
    }

    // Check for inviteCode validity... then, if valid, add the userId to the Chat and the chatId to the User.
    // If successfully, return Uni<true>
    public Uni<Boolean> inviteUserToChat(String inviteCode, String userId) {
        if (inviteCode == null || inviteCode.isEmpty()) {
            throw new IllegalArgumentException("inviteCode cannot be null or empty");
        }

        return Uni.createFrom().item(() -> {
            try{
                return new ObjectId(userId);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).flatMap(userIdObj -> {
            if (userIdObj == null) {
                return Uni.createFrom().item(false);
            }

            return this.getChatIdInsideInvite(inviteCode)
                    .flatMap(chatId -> {
                        if (chatId == null) {
                            return Uni.createFrom().item(false);
                        }

                        return chatRepository.addUserToChat(userIdObj, chatId)
                                .flatMap(userAdded -> {
                                    if (!userAdded) {
                                        return Uni.createFrom().item(false);
                                    }

                                    return userRepository.addChatIdToUser(userIdObj, chatId)
                                            .map(user -> user != null);
                                });
                    });
        });
    }

}
