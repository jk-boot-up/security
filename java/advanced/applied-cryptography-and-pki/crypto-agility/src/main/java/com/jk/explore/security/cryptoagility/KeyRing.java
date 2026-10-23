package com.jk.explore.security.cryptoagility;

import java.security.SecureRandom;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** The keys of the bank, by key id. A key id can be retired, and then nothing written under it can be read. */
public final class KeyRing {

    private final Map<Integer, byte[]> masters = new TreeMap<>();
    private int current;

    public int add() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        current = masters.isEmpty() ? 1 : masters.keySet().stream().max(Integer::compare).orElseThrow() + 1;
        masters.put(current, key);
        return current;
    }

    public int current() {
        return current;
    }

    public boolean has(int id) {
        return masters.containsKey(id);
    }

    public void retire(int id) {
        masters.remove(id);
    }

    /** A key for one purpose, derived from the master key of the id with HMAC (the idea of HKDF): one master key never serves two suites directly. */
    public byte[] derive(int id, String purpose, int bytes) {
        byte[] master = masters.get(id);
        if (master == null) {
            throw new IllegalStateException("key " + id + " is retired");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(master, "HmacSHA256"));
            byte[] out = new byte[bytes];
            int filled = 0;
            for (int block = 1; filled < bytes; block++) {
                mac.update((purpose + "/" + block).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                byte[] part = mac.doFinal();
                int n = Math.min(part.length, bytes - filled);
                System.arraycopy(part, 0, out, filled, n);
                filled += n;
            }
            return out;
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
