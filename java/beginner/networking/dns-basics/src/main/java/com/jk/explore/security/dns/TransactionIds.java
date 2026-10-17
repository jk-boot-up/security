package com.jk.explore.security.dns;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Where a resolver gets the 16-bit id and the source port of each query; a response is believed only if it repeats both. */
interface TransactionIds {

    int LOWEST_PORT = 1024;
    int PORTS = 65536 - LOWEST_PORT;

    int nextId();

    int nextPort();

    /** The old way: ids count up and every query leaves from port 53, so one guess is enough. */
    static TransactionIds sequential(int firstId, int port) {
        return new TransactionIds() {
            private int next = firstId;

            @Override
            public int nextId() {
                return next++ & 0xffff;
            }

            @Override
            public int nextPort() {
                return port;
            }
        };
    }

    /** The modern way: id and port both come from a keyed hash of a counter, so neither can be predicted from earlier queries. */
    static TransactionIds keyed(String secret) {
        return new TransactionIds() {
            private long counter;
            private int port;

            @Override
            public int nextId() {
                counter++;
                int[] values = mac(secret, counter);
                port = LOWEST_PORT + Math.floorMod(values[1], PORTS);
                return values[0] & 0xffff;
            }

            @Override
            public int nextPort() {
                return port;
            }
        };
    }

    private static int[] mac(String secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            ByteBuffer out = ByteBuffer.wrap(mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array()));
            return new int[] {out.getInt(), out.getInt()};
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
