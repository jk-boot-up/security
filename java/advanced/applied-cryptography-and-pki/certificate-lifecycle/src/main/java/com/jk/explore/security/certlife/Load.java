package com.jk.explore.security.certlife;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/** A client of the service: real TLS handshakes with the JDK's own trust manager (chain, dates and host name checked), counted. */
final class Load {

    /** The result of one handshake. */
    record Attempt(boolean ok, String why, Instant at) {
    }

    /** The count of a run. */
    record Tally(int attempts, int refused, Instant firstRefusal, String firstReason) {

        int accepted() {
            return attempts - refused;
        }
    }

    private final TrustManager[] trust;
    private final String host;
    private final int port;

    Load(X509Certificate root, String host, int port) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null, null);
        store.setCertificateEntry("root", root);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(store);
        this.trust = tmf.getTrustManagers();
        this.host = host;
        this.port = port;
    }

    Attempt once() {
        Instant at = Instant.now();
        try {
            // A new context for every handshake: a client that resumes a session never looks at the certificate again.
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trust, null);
            InetAddress address = InetAddress.getByAddress(host, InetAddress.getLoopbackAddress().getAddress());
            try (SSLSocket s = (SSLSocket) context.getSocketFactory().createSocket(address, port)) {
                SSLParameters p = s.getSSLParameters();
                p.setEndpointIdentificationAlgorithm("HTTPS");
                s.setSSLParameters(p);
                s.setSoTimeout(5000);
                s.startHandshake();
                s.getInputStream().read();
                return new Attempt(true, "ok", at);
            }
        } catch (IOException e) {
            return new Attempt(false, why(e), at);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String why(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof CertificateExpiredException) {
                return "certificate expired";
            }
        }
        return e.getClass().getSimpleName();
    }

    /** Runs the given number of handshakes, one every interval of the schedule (not one interval after the end of the last, so slow ones do not drift). */
    Tally run(int attempts, long intervalMillis) throws InterruptedException {
        long start = System.nanoTime();
        int refused = 0;
        Instant firstRefusal = null;
        String firstReason = "none";
        for (int i = 0; i < attempts; i++) {
            long wait = start + i * intervalMillis * 1_000_000L - System.nanoTime();
            if (wait > 0) {
                Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
            }
            Attempt a = once();
            if (!a.ok()) {
                refused++;
                if (firstRefusal == null) {
                    firstRefusal = a.at();
                    firstReason = a.why();
                }
            }
        }
        return new Tally(attempts, refused, firstRefusal, firstReason);
    }
}
