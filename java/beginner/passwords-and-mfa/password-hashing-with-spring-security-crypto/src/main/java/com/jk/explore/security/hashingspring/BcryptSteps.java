package com.jk.explore.security.hashingspring;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** Takes a {bcrypt} stored text apart, and redoes a login check by hand, to show what the library does inside matches. */
public final class BcryptSteps {

    /** The parts of {bcrypt}$2a$10$ + salt + hash. */
    public record Parts(String id, String version, int cost, String salt, String hash) {

        /** Bcrypt's work doubles with every step of cost. */
        public int rounds() {
            return 1 << cost;
        }
    }

    private BcryptSteps() {
    }

    public static Parts parts(String stored) {
        String id = stored.substring(0, stored.indexOf('}') + 1);     // {bcrypt}
        String[] f = stored.substring(id.length()).split("\\$");      // "", "2a", "10", salt + hash
        return new Parts(id, "$" + f[1] + "$", Integer.parseInt(f[2]), f[3].substring(0, 22), f[3].substring(22));
    }

    /** Steps 1 and 2 of a login: hash the typed password again, with the cost and salt read from the stored text. */
    public static String hashAgain(String typed, Parts stored) {
        String again = BCrypt.hashpw(typed, stored.version() + "%02d$".formatted(stored.cost()) + stored.salt());
        return again.substring(again.length() - 31);
    }

    /** Step 3: the same hash means the right password. */
    public static boolean sameHash(String again, Parts stored) {
        return MessageDigest.isEqual(again.getBytes(StandardCharsets.UTF_8), stored.hash().getBytes(StandardCharsets.UTF_8));
    }
}
