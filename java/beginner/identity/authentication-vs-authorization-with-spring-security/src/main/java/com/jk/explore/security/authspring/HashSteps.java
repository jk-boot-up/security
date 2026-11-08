package com.jk.explore.security.authspring;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * BCrypt by hand. A stored password is one line of four parts: version, cost, salt and hash.
 * Checking a login hashes the typed password again with the stored salt and cost, and compares the hashes.
 */
public final class HashSteps {

    /** The four parts of a stored BCrypt line, such as $2a$ 04 (salt, 22 letters) (hash, 31 letters). */
    public record Parts(String version, String cost, String salt, String hash) {

        static Parts of(String stored) {
            return new Parts(stored.substring(0, 4), stored.substring(4, 6), stored.substring(7, 29), stored.substring(29));
        }

        /** The whole line, as the shop stores it. */
        public String stored() {
            return version + cost + "$" + salt + hash;
        }
    }

    private HashSteps() {
    }

    /** Signing up: hash the password with a salt and a cost. Real use takes a random salt every time. */
    public static Parts hash(String password, String salt, int cost) {
        return Parts.of(BCrypt.hashpw(password, String.format("$2a$%02d$%s", cost, salt)));
    }

    /** Logging in: hash the typed password again, with the salt and the cost read from the stored line. */
    public static Parts again(String typed, Parts stored) {
        return Parts.of(BCrypt.hashpw(typed, stored.version() + stored.cost() + "$" + stored.salt()));
    }
}
