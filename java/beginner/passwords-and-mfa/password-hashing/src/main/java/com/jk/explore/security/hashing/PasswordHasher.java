package com.jk.explore.security.hashing;

/** Turns a password into the text that is stored, and checks a login attempt against it. */
public interface PasswordHasher {

    String name();

    /** What to store for this password. */
    String hash(String password);

    /** True if the password is the one that produced the stored text. */
    boolean verify(String password, String stored);

    /** How many steps of work one hash or one check costs. */
    long stepsPerHash();
}
