package com.jk.explore.security.tls;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

/**
 * Runs the online store story in five numbered acts and prints exact, repeatable results. Keys and nonces are random, so only facts that
 * do not depend on them are printed. Each act prints its input and its result in separate, labelled sections.
 */
public class TlsDemo {

    static final String ORDER = "card=4111111111111111&order=A-1001";
    static final String CARD = "4111111111111111";
    /** The JDK adds this many zero bytes after the data in every TLS 1.3 record (see the JDK's OutputRecord). */
    static final int PADDING = 16;
    /** The authentication tag that AES-GCM adds to every record. */
    static final int TAG = 16;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        SSLContext client = TestCertificates.client();
        SSLContext shop = TestCertificates.server("shop.p12");
        act1BeforeAndAfter(out, client, shop);
        act2Handshake(out, shop);
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

    private static String hex(byte[] bytes, int from, int to) {
        return HexFormat.ofDelimiter(" ").formatHex(bytes, from, to);
    }

    private static String types(byte[] bytes) {
        return Wire.recordTypes(bytes).stream().map(String::valueOf).collect(Collectors.joining(" "));
    }

    private static void act1BeforeAndAfter(StringBuilder out, SSLContext client, SSLContext shop) throws SSLException {
        line(out, "ONE. A sniffer on the wire, without and with TLS.");
        byte[] plain = ORDER.getBytes(StandardCharsets.UTF_8);
        line(out, "  BEFORE TLS (the order, as the sniffer sees it):");
        line(out, "    " + ORDER);
        line(out, "    " + plain.length + " bytes, the card number readable: " + Wire.contains(plain, CARD) + "  <- READABLE");
        byte[] record = connected(client, shop).clientSeals(plain);
        int length = record.length - 5;
        line(out, "  AFTER TLS (one record of " + record.length + " bytes, as the sniffer sees it):");
        line(out, "    header    " + hex(record, 0, 5) + "  (in the clear)");
        line(out, "    the rest  " + length + " bytes, encrypted");
        line(out, "    the card number readable: " + Wire.contains(record, CARD));
        line(out, "  HOW THE " + record.length + " BYTES ARE MADE:");
        line(out, "    " + hex(record, 0, 1) + "        type " + Wire.recordTypes(record).get(0) + ", application data");
        line(out, "    " + hex(record, 1, 3) + "     version, written as TLS 1.2 for old boxes");
        line(out, "    " + hex(record, 3, 5) + "     length: " + length + " bytes follow the header");
        line(out, "    " + length + " bytes  " + plain.length + " order + 1 type + " + PADDING + " padding + " + TAG + " tag");
    }

    private static void act2Handshake(StringBuilder out, SSLContext shop) throws Exception {
        line(out, "TWO. The handshake.");
        // a client that has never met the shop, so the whole handshake runs and the certificate is sent
        Connection connection = connected(TestCertificates.client(), shop);
        byte[] fromClient = connection.wire.clientBytes();
        byte[] fromServer = connection.wire.serverBytes();
        line(out, "  BEFORE THE KEYS (the two hellos, in the clear):");
        line(out, "    client sends  ClientHello, record type " + Wire.recordTypes(fromClient).get(0));
        line(out, "    the site name " + TestCertificates.HOST + " readable: " + Wire.contains(fromClient, TestCertificates.HOST));
        line(out, "    server sends  ServerHello, record type " + Wire.recordTypes(fromServer).get(0));
        line(out, "    chosen        " + connection.protocol() + ", " + connection.cipherSuite());
        line(out, "  AFTER THE KEYS (the rest of the handshake, encrypted):");
        line(out, "    record types from the client  " + types(fromClient));
        line(out, "    record types from the server  " + types(fromServer));
        line(out, "    the certificate readable: " + Wire.contains(fromServer, TestCertificates.HOST) + "  <- ENCRYPTED");
        line(out, "    the keys themselves were never sent");
        line(out, "  WHAT THE RECORD TYPES MEAN:");
        line(out, "    " + Wire.HANDSHAKE + "  handshake, in the clear");
        line(out, "    " + Wire.CHANGE_CIPHER_SPEC + "  change cipher spec, kept only for old boxes");
        line(out, "    " + Wire.APPLICATION_DATA + "  encrypted: the rest of the handshake, then data");
        String[] part = connection.cipherSuite().split("_");          // TLS, AES, 256, GCM, SHA384
        line(out, "  THE CIPHER SUITE, PART BY PART:");
        line(out, "    " + part[0] + "        the protocol");
        line(out, "    " + part[1] + " " + part[2] + "    the cipher, with a " + part[2] + "-bit key");
        line(out, "    " + part[3] + "        the mode: encrypts and adds a tag");
        line(out, "    " + part[4] + "     the hash that makes the keys");
    }

    private static void act3Identity(StringBuilder out, SSLContext client, SSLContext shop) throws Exception {
        line(out, "THREE. Who is on the other end.");
        String shopPrint = TestCertificates.fingerprint("shop.p12");
        String fakePrint = TestCertificates.fingerprint("impostor.p12");
        line(out, "  BEFORE THE HANDSHAKE (what the client knows):");
        line(out, "    asks for  " + TestCertificates.HOST);
        line(out, "    trusts    one certificate, SHA-256 " + shopPrint);
        line(out, "  AFTER THE HANDSHAKE (three servers answer):");
        line(out, "    1. the shop, certificate " + shopPrint);
        line(out, "       works: " + new Connection(client, shop, TestCertificates.HOST).handshake());
        SSLContext impostor = TestCertificates.server("impostor.p12");
        line(out, "    2. an impostor, same name, certificate " + fakePrint);
        line(out, "       works: " + new Connection(client, impostor, TestCertificates.HOST).handshake() + "  <- NOT TRUSTED");
        line(out, "    3. the shop, but the client asked for other.example.test");
        line(out, "       works: " + new Connection(client, shop, "other.example.test").handshake() + "  <- THE NAME MUST MATCH");
        line(out, "  a name alone proves nothing: the client must trust the certificate");
    }

    private static void act4Protection(StringBuilder out, SSLContext client, SSLContext shop) throws SSLException {
        line(out, "FOUR. Protecting the data.");
        byte[] plain = ORDER.getBytes(StandardCharsets.UTF_8);
        line(out, "  BEFORE SENDING (the order):");
        line(out, "    " + ORDER);
        Connection connection = connected(client, shop);
        byte[] record = connection.clientSeals(plain);
        byte[] second = connected(client, shop).clientSeals(plain);
        line(out, "  AFTER SENDING (the record, in two connections):");
        line(out, "    the same bytes on the wire both times: " + Arrays.equals(record, second) + "  <- FRESH KEYS");
        line(out, "    the server opens it, and gets the order back: " + Arrays.equals(connection.serverOpens(record), plain));
        Connection tampered = connected(client, shop);
        byte[] changed = tampered.clientSeals(plain);
        int at = changed.length / 2;
        changed[at] ^= 0x01;
        line(out, "  BEFORE THE CHECK (an attacker flips one bit):");
        line(out, "    byte " + at + " of " + changed.length + ", xor 01");
        String result;
        try {
            tampered.serverOpens(changed);
            result = "accepted";
        } catch (SSLException e) {
            result = "refused, " + e.getMessage();
        }
        line(out, "  AFTER THE CHECK (the server checks the tag):");
        line(out, "    " + result + "  <- CHANGE SEEN");
        line(out, "  fresh keys come from temporary key pairs, made per connection,");
        line(out, "  so a stolen server key cannot open recorded traffic");
    }

    private static String offer(Connection connection, String version) {
        connection.client.setEnabledProtocols(new String[] {version});
        boolean works = connection.handshake();
        return "client offers " + (version + "   ").substring(0, 7) + "  works: " + works + (works ? ", agreed " + connection.protocol() : "");
    }

    private static void act5SettingsAndCosts(StringBuilder out, SSLContext client, SSLContext shop) {
        line(out, "FIVE. Settings, and the bill.");
        line(out, "  BEFORE TIGHTENING (a default server):");
        line(out, "    " + offer(new Connection(client, shop, TestCertificates.HOST), "TLSv1") + "  <- OLD VERSIONS ARE OFF");
        line(out, "    " + offer(new Connection(client, shop, TestCertificates.HOST), "TLSv1.2"));
        line(out, "  AFTER TIGHTENING (the server requires TLS 1.3):");
        for (String version : List.of("TLSv1.2", "TLSv1.3")) {
            Connection strict = new Connection(client, shop, TestCertificates.HOST);
            strict.server.setEnabledProtocols(new String[] {"TLSv1.3"});
            line(out, "    " + offer(strict, version));
        }
        line(out, "  THE BILL:");
        line(out, "    certificates must be obtained, renewed and kept safe");
        line(out, "    the handshake costs a round trip and some computing");
        line(out, "    TLS protects the connection, not the ends");
        line(out, "    the site name and the message sizes still show");
    }
}
