package com.jk.explore.security.tcpip;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The server side of the three-way handshake, as a state machine. A SYN creates a half-open entry and is answered with a SYN-ACK;
 * the final ACK turns it into an established connection. The table of half-open entries has a limit (the backlog), which is
 * what a SYN flood fills. With SYN cookies the server keeps no entry: the state is encoded in its own sequence number.
 */
final class Listener {

    private final int backlog;
    private final boolean cookies;
    private final byte[] secret;
    private final SequenceNumbers numbers;
    private final Map<String, Long> halfOpen = new HashMap<>();
    private final Set<String> established = new HashSet<>();

    Listener(int backlog, boolean cookies, byte[] secret, SequenceNumbers numbers) {
        this.backlog = backlog;
        this.cookies = cookies;
        this.secret = secret;
        this.numbers = numbers;
    }

    private static String key(int address, int port) {
        return Address.text(address) + ":" + port;
    }

    /** Handles a SYN. Returns the server's sequence number if a SYN-ACK is sent, or nothing if the SYN is dropped. */
    Optional<Long> onSyn(int address, int port, long clientSequence) {
        if (cookies) {
            return Optional.of(SequenceNumbers.mac(secret, address, port, clientSequence));
        }
        if (halfOpen.size() >= backlog) {
            return Optional.empty();
        }
        long serverSequence = numbers.next(address, port);
        halfOpen.put(key(address, port), serverSequence);
        return Optional.of(serverSequence);
    }

    /** Handles the final ACK of the handshake. Returns whether a connection is now established. */
    boolean onAck(int address, int port, long clientSequence, long acknowledgement) {
        String key = key(address, port);
        if (cookies) {
            long expected = SequenceNumbers.mac(secret, address, port, clientSequence - 1);
            if (acknowledgement == ((expected + 1) & 0xffffffffL)) {
                established.add(key);
                return true;
            }
            return false;
        }
        Long serverSequence = halfOpen.get(key);
        if (serverSequence != null && acknowledgement == ((serverSequence + 1) & 0xffffffffL)) {
            halfOpen.remove(key);
            established.add(key);
            return true;
        }
        return false;
    }

    int halfOpenCount() {
        return halfOpen.size();
    }

    boolean isEstablished(int address, int port) {
        return established.contains(key(address, port));
    }
}
