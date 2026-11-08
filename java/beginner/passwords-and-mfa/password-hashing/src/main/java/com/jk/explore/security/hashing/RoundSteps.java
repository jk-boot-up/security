package com.jk.explore.security.hashing;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Redoes PBKDF2 with HMAC-SHA-256 by hand, to show what "100000 rounds" means. Round 1 is an HMAC of the salt
 * followed by the block number 1, keyed with the password; every next round is an HMAC of the round before.
 * The hash is all the rounds XORed together. A test checks it gives exactly the library's hash.
 * Only for teaching: real code uses {@link Pbkdf2Hasher}.
 */
public final class RoundSteps {

    /** The first three rounds, the last one, and the hash they make. */
    public record Steps(List<byte[]> firstRounds, byte[] lastRound, byte[] hash) {
    }

    private RoundSteps() {
    }

    public static Steps of(String password, byte[] salt, int rounds) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] first = ByteBuffer.allocate(salt.length + 4).put(salt).putInt(1).array();
            byte[] round = hmac.doFinal(first);
            byte[] hash = round.clone();
            List<byte[]> firstRounds = new java.util.ArrayList<>(List.of(round));
            for (int i = 2; i <= rounds; i++) {
                round = hmac.doFinal(round);
                hash = Hex.xor(hash, round);
                if (i <= 3) {
                    firstRounds.add(round);
                }
            }
            return new Steps(firstRounds, round, hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is part of every Java runtime", e);
        }
    }
}
