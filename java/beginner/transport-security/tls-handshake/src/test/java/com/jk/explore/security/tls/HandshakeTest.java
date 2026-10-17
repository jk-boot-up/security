package com.jk.explore.security.tls;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HandshakeTest {

    private static SSLContext client;
    private static SSLContext shop;

    @BeforeAll
    static void contexts() throws Exception {
        client = TestCertificates.client();
        shop = TestCertificates.server("shop.p12");
    }

    private static Connection connect() {
        Connection connection = new Connection(client, shop, TestCertificates.HOST);
        assertTrue(connection.handshake(), connection.failure());
        return connection;
    }

    @Test
    void handshakeAgreesTls13() {
        Connection connection = connect();
        assertEquals("TLSv1.3", connection.protocol());
        assertTrue(connection.cipherSuite().startsWith("TLS_AES_"));
    }

    @Test
    void firstRecordsAreHandshakeRecords() {
        Connection connection = connect();
        assertEquals(Wire.HANDSHAKE, Wire.recordTypes(connection.wire.clientBytes()).get(0));
        assertEquals(Wire.HANDSHAKE, Wire.recordTypes(connection.wire.serverBytes()).get(0));
    }

    @Test
    void helloShowsTheSiteNameButNotTheCertificate() {
        Connection connection = connect();
        assertTrue(Wire.contains(connection.wire.clientBytes(), TestCertificates.HOST));
        assertFalse(Wire.contains(connection.wire.serverBytes(), TestCertificates.HOST));
    }

    @Test
    void dataRoundTripsAndIsHiddenOnTheWire() throws SSLException {
        Connection connection = connect();
        byte[] order = TlsDemo.ORDER.getBytes(StandardCharsets.UTF_8);
        byte[] record = connection.clientSeals(order);
        assertEquals(List.of(Wire.APPLICATION_DATA), Wire.recordTypes(record));
        assertFalse(Wire.contains(record, "4111"));
        assertArrayEquals(order, connection.serverOpens(record));
    }

    @Test
    void everyChangedByteOfARecordIsRefused() throws SSLException {
        byte[] order = TlsDemo.ORDER.getBytes(StandardCharsets.UTF_8);
        byte[] record = connect().clientSeals(order);
        for (int i = 5; i < record.length; i++) {
            Connection connection = connect();
            byte[] changed = connection.clientSeals(order);
            changed[i] ^= 0x01;
            assertThrows(SSLException.class, () -> connection.serverOpens(changed), "byte " + i);
        }
    }

    @Test
    void sameDataInTwoConnectionsLooksDifferent() throws SSLException {
        byte[] order = TlsDemo.ORDER.getBytes(StandardCharsets.UTF_8);
        assertFalse(java.util.Arrays.equals(connect().clientSeals(order), connect().clientSeals(order)));
    }

    @Test
    void untrustedCertificateIsRefused() throws Exception {
        Connection fake = new Connection(client, TestCertificates.server("impostor.p12"), TestCertificates.HOST);
        assertFalse(fake.handshake());
        assertNotEquals(null, fake.failure());
    }

    @Test
    void wrongNameIsRefused() {
        assertFalse(new Connection(client, shop, "other.example.test").handshake());
    }

    @Test
    void tls10IsSwitchedOff() {
        Connection old = new Connection(client, shop, TestCertificates.HOST);
        old.client.setEnabledProtocols(new String[] {"TLSv1"});
        assertFalse(old.handshake());
    }

    @Test
    void tls12StillWorksUnlessTheServerRequiresTls13() {
        Connection middle = new Connection(client, shop, TestCertificates.HOST);
        middle.client.setEnabledProtocols(new String[] {"TLSv1.2"});
        assertTrue(middle.handshake());
        assertEquals("TLSv1.2", middle.protocol());
        Connection strict = new Connection(client, shop, TestCertificates.HOST);
        strict.client.setEnabledProtocols(new String[] {"TLSv1.2"});
        strict.server.setEnabledProtocols(new String[] {"TLSv1.3"});
        assertFalse(strict.handshake());
    }

    @Test
    void recordTypesReadsHeaders() {
        byte[] two = {22, 3, 3, 0, 2, 9, 9, 23, 3, 3, 0, 1, 7};
        assertEquals(List.of(22, 23), Wire.recordTypes(two));
    }
}
