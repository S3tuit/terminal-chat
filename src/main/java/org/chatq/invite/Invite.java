package org.chatq.invite;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import java.security.SecureRandom;
import java.time.Instant;

@MongoEntity
public class Invite extends PanacheMongoEntity {

    public ObjectId chatId;
    public String code;
    public Instant createdAt;
    public Instant expiresAt;

    @BsonIgnore
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    @BsonIgnore
    static final int INVITE_LENGTH = 9;
    @BsonIgnore
    static SecureRandom random = new SecureRandom();

    public Invite() {}

    public Invite(ObjectId chatId, String code, Instant createdAt, Instant expiresAt) {
        this.chatId = chatId;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // Return a random alphanumeric String of the specified length
    public static String generateRandomString(int length) {
        if (length < 1) throw new IllegalArgumentException("length cannot be less than 1");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(AB.charAt(random.nextInt(AB.length())));
        }
        return sb.toString();
    }

    public static Invite createNewInvite(ObjectId chatId, Instant expiresAt) {
        String inviteCode = Invite.generateRandomString(INVITE_LENGTH);
        Invite invite = null;

        try {
            invite = new Invite(chatId, inviteCode, Instant.now(), expiresAt);
            invite.persist();
            return invite;
        } catch (Exception e) {
            System.out.println("Oh no, something went wrong during invite creation");
            e.printStackTrace();
            return null;
        }
    }

    // Returns the chatId of the invite if it's a valid invite at db
    public static ObjectId getChatIdInsideInvite(String inviteCode) {
        Invite invite = Invite.find("code", inviteCode).firstResult();
        if (invite != null) {

            // If the invite is expired delete it at db and return false
            if (invite.expiresAt.isBefore(Instant.now())) {
                invite.delete();
                return null;
            }
            return invite.chatId;
        }
        return null;
    }
}
