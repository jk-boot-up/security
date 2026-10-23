package com.jk.explore.security.tlsattacks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import org.junit.jupiter.api.Test;

class TapTest {

    /** A target that answers every connection with the same bytes it was sent. */
    static ServerSocket echo() throws IOException {
        ServerSocket target = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        Thread.ofVirtual().start(() -> {
            try (Socket c = target.accept()) {
                byte[] buf = new byte[4096];
                int n = c.getInputStream().read(buf);
                c.getOutputStream().write(buf, 0, n);
            } catch (IOException closed) {
                // test over
            }
        });
        return target;
    }

    @Test
    void aHandshakeAboveTheLimitIsBrokenAndRecorded() throws Exception {
        try (ServerSocket target = echo(); Tap tap = new Tap(target.getLocalPort(), 0x0301)) {
            try (Socket s = new Socket(InetAddress.getLoopbackAddress(), tap.port())) {
                s.getOutputStream().write(TlsRecordsTest.clientHelloRecord(0x0303, 0));
                assertEquals(-1, s.getInputStream().read());
            }
            List<Tap.Capture> c = tap.await(1);
            assertTrue(c.get(0).broken());
            assertEquals(0, c.get(0).serverToClient().length);
        }
    }

    @Test
    void aHandshakeAtTheLimitIsForwardedAndBothDirectionsAreRecorded() throws Exception {
        byte[] hello = TlsRecordsTest.clientHelloRecord(0x0301, 0);
        try (ServerSocket target = echo(); Tap tap = new Tap(target.getLocalPort(), 0x0301)) {
            try (Socket s = new Socket(InetAddress.getLoopbackAddress(), tap.port())) {
                s.getOutputStream().write(hello);
                assertArrayEquals(hello, s.getInputStream().readNBytes(hello.length));
            }
            Tap.Capture c = tap.await(1).get(0);
            assertFalse(c.broken());
            assertArrayEquals(hello, c.clientToServer());
            assertArrayEquals(hello, c.serverToClient());
        }
    }
}
