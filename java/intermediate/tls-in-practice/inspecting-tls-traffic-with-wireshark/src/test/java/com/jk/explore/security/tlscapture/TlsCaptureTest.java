package com.jk.explore.security.tlscapture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real nginx, the openssl client, a tap and tshark. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TlsCaptureTest {

    private static final String HOST = "www.shop.localhost";

    private Path work;
    private LocalCa ca;
    private Nginx modern;
    private Nginx rsa;
    private Path pcap1;
    private Path keys1;
    private Path pcap2;
    private Path keys2;
    private Path tls12;

    @BeforeAll
    void start() throws Exception {
        work = Files.createTempDirectory("tlscapture-test");
        ca = new LocalCa(work.resolve("ca"));
        ca.issue("www", "-1h", "2160h", false, HOST);
        ca.issue("rsa", "-1h", "2160h", true, HOST);
        modern = new Nginx(work.resolve("modern"));
        rsa = new Nginx(work.resolve("rsa"));
        modern.start("TLSv1.3", ca.cert("www").toString(), ca.key("www").toString());
        rsa.start("TLSv1.2", ca.cert("rsa").toString(), ca.key("rsa").toString());
        pcap1 = work.resolve("one.pcap");
        keys1 = work.resolve("one.keys");
        pcap2 = work.resolve("two.pcap");
        keys2 = work.resolve("two.keys");
        tls12 = work.resolve("tls12.pcap");
        capture(modern.port(), "-tls1_3", pcap1, keys1);
        capture(modern.port(), "-tls1_3", pcap2, keys2);
        capture(rsa.port(), "-tls1_2", tls12, null);
    }

    private String capture(int port, String protocol, Path pcap, Path keys) throws Exception {
        try (Tap tap = new Tap(port, pcap)) {
            String page = Client.login(tap.port(), ca.root(), protocol, keys);
            tap.awaitCapture();
            return page;
        }
    }

    @AfterAll
    void stop() throws Exception {
        modern.close();
        rsa.close();
    }

    @Test
    void theLoginWorkedThroughTheTap() throws Exception {
        try (Tap tap = new Tap(modern.port(), work.resolve("check.pcap"))) {
            String page = Client.login(tap.port(), ca.root(), "-tls1_3", null);
            tap.awaitCapture();
            assertTrue(page.contains("welcome ana") && page.contains("session=7f3a"));
        }
    }

    @Test
    void withoutSecretsTheCaptureShowsTheNameAndNothingElse() throws Exception {
        assertEquals(HOST, Tshark.fields(pcap1, "tls.handshake.type == 1", List.of(), "tls.handshake.extensions_server_name").get(0).get(0));
        assertEquals(0, Tshark.count(pcap1, "http", List.of()));
        assertEquals(0, Tshark.count(pcap1, "tls.handshake.type == 11", List.of()));
        assertFalse(new String(Files.readAllBytes(pcap1), java.nio.charset.StandardCharsets.ISO_8859_1).contains("hunter2"));
    }

    @Test
    void theKeyLogHasFiveLinesAndNoPrivateKey() throws Exception {
        List<String> lines = Files.readAllLines(keys1).stream().filter(l -> !l.startsWith("#")).toList();
        assertEquals(5, lines.size());
        assertTrue(lines.stream().noneMatch(l -> l.contains("BEGIN")));
        assertEquals(64, lines.get(0).split(" ")[1].length());
    }

    @Test
    void withTheKeyLogTheFormAndTheCertificateAreVisible() throws Exception {
        List<List<String>> request = Tshark.fields(pcap1, "http.request", Tshark.keyLog(keys1), "http.request.uri", "urlencoded-form.value");
        assertEquals("/login", request.get(0).get(0));
        assertEquals("ana,hunter2", request.get(0).get(1));
        assertTrue(Tshark.count(pcap1, "tls.handshake.type == 11", Tshark.keyLog(keys1)) > 0);
    }

    @Test
    void aKeyLogOpensOnlyItsOwnConnection() throws Exception {
        assertEquals(0, Tshark.count(pcap2, "http", Tshark.keyLog(keys1)));
        assertTrue(Tshark.count(pcap2, "http", Tshark.keyLog(keys2)) > 0);
    }

    @Test
    void aTlsOneTwoCertificateIsInTheClear() throws Exception {
        assertTrue(Tshark.count(tls12, "tls.handshake.type == 11", List.of()) > 0);
    }

    @Test
    void theServerKeyOpensNothingThatUsedEphemeralKeys() throws Exception {
        assertEquals(0, Tshark.count(tls12, "http", Tshark.serverKey(ca.key("rsa"))));
    }

    @Test
    void theServerKeyOpensAnRsaKeyExchangeRecording() throws Exception {
        try (LegacyServer legacy = new LegacyServer(ca.pkcs12("rsa"))) {
            Path rsaKex = work.resolve("rsakex.pcap");
            try (Tap tap = new Tap(legacy.port(), rsaKex)) {
                javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");
                java.security.KeyStore store = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
                store.load(null, null);
                try (var in = Files.newInputStream(ca.root())) {
                    store.setCertificateEntry("root", java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(in));
                }
                javax.net.ssl.TrustManagerFactory trust = javax.net.ssl.TrustManagerFactory.getInstance("PKIX");
                trust.init(store);
                context.init(null, trust.getTrustManagers(), null);
                try (javax.net.ssl.SSLSocket socket = (javax.net.ssl.SSLSocket) context.getSocketFactory().createSocket(java.net.InetAddress.getLoopbackAddress(), tap.port())) {
                    socket.setEnabledProtocols(new String[] {"TLSv1.2"});
                    socket.setEnabledCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA"});
                    socket.startHandshake();
                    socket.getOutputStream().write(("POST /login HTTP/1.1\r\nHost: " + HOST + "\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: 25\r\nConnection: close\r\n\r\n" + Client.BODY).getBytes());
                    socket.getInputStream().readAllBytes();
                }
                tap.awaitCapture();
            }
            assertEquals("ana,hunter2", Tshark.fields(rsaKex, "http.request", Tshark.serverKey(ca.key("rsa")), "urlencoded-form.value").get(0).get(0));
        }
    }
}
