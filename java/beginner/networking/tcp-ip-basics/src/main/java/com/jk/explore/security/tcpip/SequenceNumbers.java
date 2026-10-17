package com.jk.explore.security.tcpip;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Where a connection's first sequence number comes from. */
interface SequenceNumbers {

    long next(int clientAddress, int clientPort);

    /** The old way: a counter that goes up by a fixed step for every connection, so the next number can be guessed. */
    static SequenceNumbers counter(long start, long step) {
        return new SequenceNumbers() {
            private long current = start - step;

            @Override
            public long next(int clientAddress, int clientPort) {
                current = (current + step) & 0xffffffffL;
                return current;
            }
        };
    }

    /** The modern way (RFC 6528): a keyed hash of the connection's addresses and ports plus a counter, so it cannot be guessed. */
    static SequenceNumbers keyed(byte[] secret) {
        return new SequenceNumbers() {
            private long ticks;

            @Override
            public long next(int clientAddress, int clientPort) {
                ticks++;
                return mac(secret, clientAddress, clientPort, ticks);
            }
        };
    }

    static long mac(byte[] secret, int address, int port, long extra) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] out = mac.doFinal(ByteBuffer.allocate(16).putInt(address).putInt(port).putLong(extra).array());
            return ByteBuffer.wrap(out).getInt() & 0xffffffffL;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
