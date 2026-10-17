package com.jk.explore.security.tcpip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ListenerTest {

    private static final byte[] SECRET = "test-secret".getBytes(StandardCharsets.UTF_8);

    private Listener plain(int backlog) {
        return new Listener(backlog, false, SECRET, SequenceNumbers.keyed(SECRET));
    }

    @Test
    void handshakeEstablishesAConnection() {
        Listener listener = plain(4);
        long serverSequence = listener.onSyn(1, 1000, 50).orElseThrow();
        assertEquals(1, listener.halfOpenCount());
        assertTrue(listener.onAck(1, 1000, 51, serverSequence + 1));
        assertEquals(0, listener.halfOpenCount());
        assertTrue(listener.isEstablished(1, 1000));
    }

    @Test
    void wrongAcknowledgementIsRefused() {
        Listener listener = plain(4);
        long serverSequence = listener.onSyn(1, 1000, 50).orElseThrow();
        assertFalse(listener.onAck(1, 1000, 51, serverSequence + 2));
        assertFalse(listener.isEstablished(1, 1000));
    }

    @Test
    void ackWithoutSynIsRefused() {
        assertFalse(plain(4).onAck(1, 1000, 51, 9));
    }

    @Test
    void fullTableDropsNewSyns() {
        Listener listener = plain(2);
        assertTrue(listener.onSyn(1, 1, 1).isPresent());
        assertTrue(listener.onSyn(2, 2, 1).isPresent());
        assertTrue(listener.onSyn(3, 3, 1).isEmpty());
    }

    @Test
    void cookiesKeepNoStateAndStillWork() {
        Listener listener = new Listener(1, true, SECRET, SequenceNumbers.keyed(SECRET));
        for (int i = 0; i < 20; i++) {
            listener.onSyn(100 + i, 5, 7);
        }
        assertEquals(0, listener.halfOpenCount());
        long serverSequence = listener.onSyn(1, 1000, 50).orElseThrow();
        assertTrue(listener.onAck(1, 1000, 51, serverSequence + 1));
    }

    @Test
    void cookieForAnotherClientDoesNotWork() {
        Listener listener = new Listener(1, true, SECRET, SequenceNumbers.keyed(SECRET));
        long serverSequence = listener.onSyn(1, 1000, 50).orElseThrow();
        assertFalse(listener.onAck(2, 1000, 51, serverSequence + 1));
    }

    @Test
    void counterNumbersCanBeGuessedAndKeyedOnesCannot() {
        SequenceNumbers counter = SequenceNumbers.counter(1000, 64000);
        long first = counter.next(1, 1);
        assertEquals(first + 64000, counter.next(2, 2));
        SequenceNumbers keyed = SequenceNumbers.keyed(SECRET);
        long a = keyed.next(1, 1);
        assertTrue(keyed.next(2, 2) != a + 64000);
    }

    @Test
    void realConnectionCarriesBytesInTheClear() throws Exception {
        PlaintextTap.Result result = PlaintextTap.send("card=4111");
        assertTrue(result.connected());
        assertTrue(result.serverReceivedAll());
        assertTrue(result.seenOnTheWire().contains("4111"));
    }
}
