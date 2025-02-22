package org.chatq.invite;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import java.security.SecureRandom;
import java.time.Instant;

@MongoEntity
public class Invite extends ReactivePanacheMongoEntity {

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

}
