package com.jk.explore.security.mtls;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.security.cert.X509Certificate;

/** A service calling another over HTTPS with the certificate it holds (or none). */
final class Caller {

    /** refused: no HTTP answer came back, the TLS connection was refused or broke. serverFingerprint: SHA-256 of the certificate the server showed. */
    record Reply(boolean refused, int status, String body, String serverFingerprint) {

        String line() {
            return refused ? "handshake refused" : String.valueOf(status);
        }
    }

    private Caller() {
    }

    /** A plain HTTP call, for a service that trusts the network. */
    static Reply callPlain(String url, String method) throws InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest.BodyPublisher body = method.equals("POST") ? HttpRequest.BodyPublishers.ofString("amount=10") : HttpRequest.BodyPublishers.noBody();
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url)).method(method, body).build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(false, response.statusCode(), response.body(), "");
        } catch (IOException e) {
            return new Reply(true, 0, "", "");
        }
    }

    static Reply call(Credential credential, List<X509Certificate> trusted, String url, String method) throws GeneralSecurityException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder().sslContext(Tls.context(credential, trusted)).connectTimeout(Duration.ofSeconds(5)).build()) {
            HttpRequest.BodyPublisher body = method.equals("POST") ? HttpRequest.BodyPublishers.ofString("amount=10") : HttpRequest.BodyPublishers.noBody();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).method(method, body).timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Certificate shown = response.sslSession().orElseThrow().getPeerCertificates()[0];
            return new Reply(false, response.statusCode(), response.body(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(shown.getEncoded())));
        } catch (IOException e) {
            return new Reply(true, 0, "", "");
        }
    }
}
