package com.jk.explore.security.mtls;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;

/** The bank's ledger service: HTTPS that demands a client certificate and decides by the identity in it. */
final class Ledger implements AutoCloseable {

    private final Path certificate;
    private final Path key;
    private final Path trustBundle;
    private final Policy policy = Policy.ledger();
    private final HttpsServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final Tls.Live tls;
    private final java.util.concurrent.atomic.AtomicInteger served = new java.util.concurrent.atomic.AtomicInteger();
    private volatile Set<BigInteger> revoked = Set.of();

    Ledger(Path certificate, Path pkcs8Key, Path trustBundle) throws IOException, GeneralSecurityException {
        this.certificate = certificate;
        this.key = pkcs8Key;
        this.trustBundle = trustBundle;
        server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        tls = new Tls.Live(Credential.load(certificate, key), Credential.readCertificates(trustBundle));
        server.setHttpsConfigurator(configurator());
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    /** Reads the certificate, the key and the trust file again; the server keeps running and new connections use the new ones. */
    void reload() throws IOException, GeneralSecurityException {
        tls.replace(Credential.load(certificate, key), Credential.readCertificates(trustBundle));
    }

    /** From now on a certificate with one of these serial numbers is refused. */
    void revoke(Set<BigInteger> serials) {
        revoked = Set.copyOf(serials);
    }

    private HttpsConfigurator configurator() {
        return new HttpsConfigurator(tls.context()) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters p = getSSLContext().getDefaultSSLParameters();
                p.setNeedClientAuth(true);
                params.setSSLParameters(p);
            }
        };
    }

    /** How many requests got as far as the ledger's own code. */
    int served() {
        return served.get();
    }

    private void handle(HttpExchange exchange) throws IOException {
        served.incrementAndGet();
        int status;
        String body;
        try {
            X509Certificate caller = (X509Certificate) ((HttpsExchange) exchange).getSSLSession().getPeerCertificates()[0];
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String identity = Identities.of(caller).orElse(null);
            if (identity == null) {
                status = 403;
                body = "no service identity in the certificate";
            } else if (revoked.contains(caller.getSerialNumber())) {
                status = 403;
                body = "certificate revoked";
            } else if (!policy.knows(method, path)) {
                status = 404;
                body = "no such endpoint";
            } else if (policy.allows(identity, method, path)) {
                status = 200;
                body = "done for " + identity;
            } else {
                status = 403;
                body = identity + " may not " + method + " " + path;
            }
        } catch (SSLPeerUnverifiedException e) {
            status = 401;
            body = "no client certificate";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
