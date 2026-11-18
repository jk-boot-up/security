package com.jk.explore.security.tlscapture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import org.junit.jupiter.api.Test;

/** The Client Hello read by hand, from a real one the JDK writes: no program and no network. */
class ClientHelloTest {

    /** The first record a JDK client sends, asking for {@code name}. */
    private static byte[] jdkClientHello(String name) throws Exception {
        SSLEngine engine = SSLContext.getDefault().createSSLEngine(name, 443);
        engine.setUseClientMode(true);
        SSLParameters p = engine.getSSLParameters();
        p.setServerNames(List.of(new SNIHostName(name)));
        engine.setSSLParameters(p);
        ByteBuffer out = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        engine.wrap(ByteBuffer.allocate(0), out);
        return Arrays.copyOf(out.array(), out.position());
    }

    @Test
    void theNameAndTheRandomAreInTheClear() throws Exception {
        byte[] bytes = jdkClientHello("www.shop.localhost");
        ClientHello hello = ClientHello.parse(bytes);
        assertEquals(ClientHello.HANDSHAKE, hello.recordType());
        assertEquals(ClientHello.CLIENT_HELLO, hello.messageType());
        assertEquals("www.shop.localhost", hello.serverName());
        assertEquals(64, hello.random().length(), "32 bytes in hex");
        assertEquals(java.util.HexFormat.of().formatHex(Arrays.copyOfRange(bytes, 11, 43)), hello.random(), "the random follows the headers and the version");
    }

    @Test
    void everyConnectionHasANewRandom() throws Exception {
        assertEquals(false, ClientHello.parse(jdkClientHello("www.shop.localhost")).random().equals(ClientHello.parse(jdkClientHello("www.shop.localhost")).random()));
    }

    @Test
    void aHelloSplitOverPacketsIsPutBackTogether() throws Exception {
        byte[] hello = jdkClientHello("pay.shop.localhost");
        byte[] padded = Arrays.copyOf(hello, hello.length + 2000);
        Path file = Files.createTempFile("hello", ".pcap");
        try (Pcap pcap = new Pcap(file, 50000, 443)) {
            pcap.data(true, padded, padded.length);
            pcap.data(false, new byte[100], 100);
        }
        assertEquals(padded.length, ClientHello.clientBytes(file).length, "only the client's bytes, from three packets");
        assertEquals("pay.shop.localhost", ClientHello.read(file).serverName());
    }
}
