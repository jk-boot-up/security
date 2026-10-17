package com.jk.explore.security.httpsnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** These tests start real nginx containers, so they need a running Docker: run them with ./gradlew integrationTest. */
@Tag("docker")
class NginxContainerTest {

    @Test
    void tlsIsEndedByNginxWithHttp2AndAllFiveHeaders() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            TlsClient.Reply reply = TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting());
            assertEquals(200, reply.status());
            assertEquals("TLSv1.3", reply.protocol());
            assertEquals(HttpClient.Version.HTTP_2, reply.version());
            assertEquals(5, HttpsNginxDemo.score(reply.headers()));
            assertEquals("nginx", reply.headers().get("server"));
        }
    }

    @Test
    void tls12IsRefusedUnlessAllowed() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            assertTrue(TlsClient.refused(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.2")));
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.COMPAT)) {
            assertEquals("TLSv1.2", TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting().withProtocols("TLSv1.2")).protocol());
        }
    }

    @Test
    void thePlainPortOnlyRedirectsAndNeverSendsHsts() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            Probe.Reply reply = Probe.get(nginx.httpPort(), "/orders?id=7");
            assertEquals(301, reply.status());
            assertEquals("https://shop.example.test/orders?id=7", reply.headers().get("location"));
            assertFalse(reply.headers().containsKey("strict-transport-security"));
            assertEquals(301, Probe.get(nginx.httpPort(), "/app").status());
        }
    }

    @Test
    void errorPagesOverHttpsStillCarryTheHeaders() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            TlsClient.Reply reply = TlsClient.get(nginx.httpsPort(), "/anything", TlsClient.Options.trusting());
            assertTrue(reply.headers().containsKey("strict-transport-security"));
        }
    }

    @Test
    void trustAndHostNameAreChecked() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            int port = nginx.httpsPort();
            assertThrows(SSLException.class, () -> TlsClient.get(port, "/", TlsClient.Options.trusting().withDefaultTrust()));
            assertThrows(SSLException.class, () -> TlsClient.get(port, "/", TlsClient.Options.trusting().withHost("127.0.0.1")));
            assertEquals(200, TlsClient.get(port, "/", TlsClient.Options.trusting()).status());
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.IMPOSTOR)) {
            assertThrows(SSLException.class, () -> TlsClient.get(nginx.httpsPort(), "/", TlsClient.Options.trusting()));
        }
    }

    @Test
    void mutualTlsNeedsAClientCertificateAndNginxSetsTheIdentityItself() throws Exception {
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.MTLS)) {
            int port = nginx.httpsPort();
            assertTrue(TlsClient.refused(port, "/", TlsClient.Options.trusting()));
            TlsClient.Reply ana = TlsClient.get(port, "/", TlsClient.Options.trusting().withClientCertificate().withHeader("X-Client-DN", "CN=Admin"));
            assertEquals(200, ana.status());
            assertTrue(ana.body().contains("client=CN=Ana the customer"));
            assertFalse(ana.body().contains("Admin"));
        }
    }

    @Test
    void copyingTheClientsForwardedProtoDeceivesTheApplication() throws Exception {
        String forged = "GET /app HTTP/1.1\r\nHost: shop.example.test\r\nX-Forwarded-Proto: https\r\nConnection: close\r\n\r\n";
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.PROXY_TRAP)) {
            assertTrue(Probe.send(nginx.httpPort(), forged).body().contains("proto=https"));
        }
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.PROXY_SAFE)) {
            assertTrue(Probe.send(nginx.httpPort(), forged).body().contains("proto=http "));
        }
    }

    @Test
    void nginxTestAcceptsTheCarefulConfigAndRefusesAMissingCertificate() throws Exception {
        String good = Nginx.configTest(NginxConfig.text(NginxConfig.Variant.CAREFUL));
        assertTrue(good.contains("syntax is ok") && good.contains("exit code 0"), good);
        String broken = Nginx.configTest(NginxConfig.text(NginxConfig.Variant.CAREFUL, "missing"));
        assertTrue(broken.contains("exit code 1"), broken);
    }

    @Test
    void noContainersAreLeftBehind() throws IOException, InterruptedException {
        try (Nginx nginx = Nginx.start(NginxConfig.Variant.CAREFUL)) {
            assertTrue(nginx.httpsPort() > 0);
        }
        Process p = new ProcessBuilder("docker", "ps", "-a", "--filter", "name=https-nginx-", "--format", "{{.Names}}").redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes()).strip();
        assertTrue(p.waitFor(30, TimeUnit.SECONDS));
        assertEquals("", out);
    }
}
