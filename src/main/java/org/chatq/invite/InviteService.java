package org.chatq.invite;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.chatq.users.UserService;

import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class InviteService {

    @Inject
    UserService userService;

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


    public Invite createInvite(ObjectId chatId, TimePeriod timePeriod) {
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
        return Invite.createNewInvite(chatId, expiresAt);
    }

    public boolean inviteUserToChat(String inviteCode, String userId) {
        if (inviteCode == null || inviteCode.isEmpty()) {
            throw new IllegalArgumentException("inviteCode cannot be null or empty");
        }
        ObjectId chatId = Invite.getChatIdInsideInvite(inviteCode);

        if (chatId != null) {
            return userService.addChatToUser(userId, chatId);
        }
        return false;
    }
}
