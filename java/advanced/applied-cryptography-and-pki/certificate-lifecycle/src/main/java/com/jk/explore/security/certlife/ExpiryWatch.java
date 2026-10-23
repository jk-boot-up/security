package com.jk.explore.security.certlife;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * An expiry monitor for one live endpoint: it connects like a scanner (it trusts nothing, it only reads the certificate that is presented)
 * and raises an alert when less than a quarter of the certificate's life is left.
 */
final class ExpiryWatch implements AutoCloseable {

    private final int port;
    private final Thread thread;
    private volatile boolean running = true;
    private volatile Instant firstAlert;

    ExpiryWatch(int port) {
        this.port = port;
        this.thread = Thread.ofPlatform().daemon().start(this::loop);
    }

    /** The leaf certificate a server presents. */
    static X509Certificate presented(int port) throws IOException, GeneralSecurityException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] {new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // not used by a client
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // a scanner reads the certificate and judges it itself
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, null);
        try (SSLSocket s = (SSLSocket) context.getSocketFactory().createSocket(InetAddress.getLoopbackAddress(), port)) {
            s.setSoTimeout(5000);
            s.startHandshake();
            return (X509Certificate) s.getSession().getPeerCertificates()[0];
        }
    }

    /** Whether a quarter of the life or less is left at the given moment. */
    static boolean shouldAlert(Instant notBefore, Instant notAfter, Instant now) {
        Duration life = Duration.between(notBefore, notAfter);
        Duration left = Duration.between(now, notAfter);
        return left.compareTo(life.dividedBy(4)) <= 0;
    }

    Instant firstAlert() {
        return firstAlert;
    }

    private void loop() {
        while (running) {
            try {
                X509Certificate cert = presented(port);
                if (firstAlert == null && shouldAlert(cert.getNotBefore().toInstant(), cert.getNotAfter().toInstant(), Instant.now())) {
                    firstAlert = Instant.now();
                }
            } catch (IOException | GeneralSecurityException e) {
                // the endpoint did not answer this time
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void close() throws InterruptedException {
        running = false;
        thread.interrupt();
        thread.join();
    }
}
