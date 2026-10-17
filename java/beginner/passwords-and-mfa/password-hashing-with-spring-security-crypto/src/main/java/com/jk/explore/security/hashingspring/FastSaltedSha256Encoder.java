package com.jk.explore.security.hashingspring;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A SHA-256 hash with a random salt, in the form {@code salt$hash}. The salt makes equal passwords look different, but SHA-256 is
 * built to be fast, so each guess costs one cheap step. Written out here to show why a salt alone is not enough.
 */
public class FastSaltedSha256Encoder implements PasswordEncoder {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String encode(CharSequence rawPassword) {
        byte[] salt = new byte[8];
        random.nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        return saltHex + "$" + hash(saltHex, rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        String saltHex = encodedPassword.substring(0, encodedPassword.indexOf('$'));
        return MessageDigest.isEqual((saltHex + "$" + hash(saltHex, rawPassword)).getBytes(StandardCharsets.UTF_8),
                encodedPassword.getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(String saltHex, CharSequence password) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((saltHex + password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }
}
