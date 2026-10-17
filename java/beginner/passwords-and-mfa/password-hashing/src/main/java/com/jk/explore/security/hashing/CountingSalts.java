package com.jk.explore.security.hashing;

import java.util.Arrays;

/**
 * Salts that are different for every call but the same on every run: 1, 2, 3 and so on, spread over 16
 * bytes. Only for the demo and the tests, so their output can be compared. They are not random and must
 * never be used for real accounts.
 */
public class CountingSalts implements SaltSource {

    private int counter;

    @Override
    public byte[] next() {
        byte[] salt = new byte[16];
        Arrays.fill(salt, (byte) 0xA5);
        counter++;
        salt[12] = (byte) (counter >>> 24);
        salt[13] = (byte) (counter >>> 16);
        salt[14] = (byte) (counter >>> 8);
        salt[15] = (byte) counter;
        return salt;
    }
}
