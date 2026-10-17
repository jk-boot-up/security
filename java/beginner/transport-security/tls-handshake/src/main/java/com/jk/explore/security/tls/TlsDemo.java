package com.jk.explore.security.tls;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. Keys and nonces are random, so only facts that do not depend on them are printed. */
public class TlsDemo {

    static final String ORDER = "card=4111111111111111&order=A-1001";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        SSLContext client = TestCertificates.client();
        SSLContext shop = TestCertificates.server("shop.p12");
        act1BeforeAndAfter(out, client, shop);
        act2Handshake(out, client, shop);
        act3Identity(out, client, shop);
        act4Protection(out, client, shop);
        act5SettingsAndCosts(out, client, shop);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static Connection connected(SSLContext client, SSLContext server) {
        Connection connection = new Connection(client, server, TestCertificates.HOST);
        if (!connection.handshake()) {
            throw new IllegalStateException("handshake failed: " + connection.failure());
        }
        return connection;
    }

    private static void act1BeforeAndAfter(StringBuilder out, SSLContext client, SSLContext shop) throws SSLException {
        line(out, "ONE. A sniffer on the wire, without and with TLS.");
        byte[] plain = ORDER.getBytes(StandardCharsets.UTF_8);
        line(out, "  without TLS the order is sent as it is, and the sniffer sees the card number: " + Wire.contains(plain, "4111111111111111"));
        Connection connection = connected(client, shop);
        byte[] record = connection.clientSeals(plain);
        line(out, "  with TLS the sniffer sees a record of type " + Wire.recordTypes(record).get(0) + " (application data) and the card number: "
                + Wire.contains(record, "4111111111111111"));
        line(out, "  the record is not larger than the order plus a few dozen bytes of header, padding and tag: " + (record.length - plain.length < 48));
    }

    private static void act2Handshake(StringBuilder out, SSLContext client, SSLContext shop) {
        line(out, "TWO. The handshake.");
        Connection connection = connected(client, shop);
        List<Integer> first = Wire.recordTypes(connection.wire.clientBytes());
        List<Integer> reply = Wire.recordTypes(connection.wire.serverBytes());
        line(out, "  the client speaks first, with a handshake record (type " + first.get(0) + ") called ClientHello");
        line(out, "  the ClientHello is not encrypted, because no key exists yet, so the sniffer reads the name of the site: "
                + Wire.contains(connection.wire.clientBytes(), TestCertificates.HOST));
        line(out, "  the server answers with a handshake record (type " + reply.get(0) + ") called ServerHello, which chooses the protocol and cipher");
        line(out, "  after the ServerHello the rest of the handshake is encrypted: the certificate is hidden from the sniffer: "
                + !Wire.contains(connection.wire.serverBytes(), TestCertificates.HOST));
        line(out, "  agreed protocol: " + connection.protocol());
        line(out, "  agreed cipher suite: " + connection.cipherSuite());
        line(out, "  both sides now share keys that were never sent over the wire");
    }

    private static void act3Identity(StringBuilder out, SSLContext client, SSLContext shop) throws Exception {
        line(out, "THREE. Who is on the other end.");
        line(out, "  the shop's own certificate, name " + TestCertificates.HOST + ": handshake works: " + new Connection(client, shop, TestCertificates.HOST).handshake());
        SSLContext impostor = TestCertificates.server("impostor.p12");
        Connection fake = new Connection(client, impostor, TestCertificates.HOST);
        line(out, "  an impostor with the same name but a certificate the client does not trust: handshake works: " + fake.handshake()
                + "  <- THE CLIENT REFUSES");
        Connection wrongName = new Connection(client, shop, "other.example.test");
        line(out, "  the shop's real certificate, but the client asked for another.example.test: handshake works: " + wrongName.handshake()
                + "  <- THE NAME MUST MATCH");
        line(out, "  a certificate proves a key belongs to a name, because someone the client trusts vouched for it");
    }

    private static void act4Protection(StringBuilder out, SSLContext client, SSLContext shop) throws SSLException {
        line(out, "FOUR. Protecting the data.");
        byte[] plain = ORDER.getBytes(StandardCharsets.UTF_8);
        Connection connection = connected(client, shop);
        byte[] record = connection.clientSeals(plain);
        line(out, "  the server opens an untouched record and gets the order back: " + Arrays.equals(connection.serverOpens(record), plain));
        Connection other = connected(client, shop);
        Connection again = connected(client, shop);
        byte[] first = other.clientSeals(plain);
        byte[] second = again.clientSeals(plain);
        line(out, "  the same order in two connections gives different bytes on the wire, because each has fresh keys: " + !Arrays.equals(first, second));
        Connection tampered = connected(client, shop);
        byte[] changed = tampered.clientSeals(plain);
        changed[changed.length / 2] ^= 0x01;
        String result;
        try {
            tampered.serverOpens(changed);
            result = "accepted";
        } catch (SSLException e) {
            result = "refused";
        }
        line(out, "  one changed bit in a record: " + result + "  <- TLS PROTECTS INTEGRITY AS WELL AS SECRECY");
        line(out, "  the keys of each connection are made fresh from temporary key pairs, so a stolen server key cannot open recorded traffic");
    }

    private static void act5SettingsAndCosts(StringBuilder out, SSLContext client, SSLContext shop) {
        line(out, "FIVE. Settings, and the bill.");
        Connection old = new Connection(client, shop, TestCertificates.HOST);
        old.client.setEnabledProtocols(new String[] {"TLSv1"});
        line(out, "  a client that only offers TLS 1.0: handshake works: " + old.handshake() + "  <- OLD VERSIONS ARE SWITCHED OFF");
        Connection middle = new Connection(client, shop, TestCertificates.HOST);
        middle.client.setEnabledProtocols(new String[] {"TLSv1.2"});
        boolean twelve = middle.handshake();
        line(out, "  a client that offers only TLS 1.2: handshake works: " + twelve + ", agreed protocol: " + (twelve ? middle.protocol() : "none"));
        Connection strict = new Connection(client, shop, TestCertificates.HOST);
        strict.server.setEnabledProtocols(new String[] {"TLSv1.3"});
        strict.client.setEnabledProtocols(new String[] {"TLSv1.2"});
        line(out, "  the same client against a server that requires TLS 1.3: handshake works: " + strict.handshake());
        line(out, "  the bill: certificates must be obtained and renewed, the handshake costs a round trip and some computing, and the keys must be kept safe");
        line(out, "  TLS protects the connection between two ends, not what the ends do with the data, and the name of the site and the sizes of messages still show");
    }
}
