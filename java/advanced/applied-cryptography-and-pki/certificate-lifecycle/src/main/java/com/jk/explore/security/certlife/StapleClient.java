package com.jk.explore.security.certlife;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 * A JDK TLS client that asks for a stapled OCSP response and decides about revocation itself: the response the server sent is the only
 * source it is given, unless the policy lets it ask the responder named in the certificate.
 */
final class StapleClient {

    /** What one connection showed. */
    record Outcome(Verdict verdict, int stapledResponses) {
    }

    private final X509Certificate root;
    private final Chains.Policy policy;
    private final boolean useStaple;
    private final Instant clock;

    StapleClient(X509Certificate root, Chains.Policy policy, boolean useStaple, Instant clock) {
        this.clock = clock;
        this.root = root;
        this.policy = policy;
        this.useStaple = useStaple;
    }

    Outcome connect(int port) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        Decider decider = new Decider();
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] {decider}, null);
        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(10_000);
            socket.startHandshake();
            return new Outcome(decider.verdict, decider.stapled);
        } catch (IOException refused) {
            if (decider.verdict == null) {
                throw refused;
            }
            return new Outcome(decider.verdict, decider.stapled);
        }
    }

    private final class Decider extends X509ExtendedTrustManager {

        private Verdict verdict;
        private int stapled;

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            List<byte[]> responses = socket instanceof SSLSocket s && s.getHandshakeSession() instanceof ExtendedSSLSession e
                    ? e.getStatusResponses() : List.of();
            stapled = responses.size();
            Map<X509Certificate, byte[]> map = useStaple && !responses.isEmpty() ? Map.of(chain[0], responses.getFirst()) : Map.of();
            verdict = Chains.validate(chain[0], List.of(chain).subList(1, chain.length), root, clock, Chains.Source.OCSP, policy, List.of(), map);
            if (!verdict.accepted()) {
                throw new CertificateException(verdict.why());
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("a socket is needed to read the stapled response");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            throw new CertificateException("a socket is needed to read the stapled response");
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            throw new CertificateException("no client certificates here");
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            throw new CertificateException("no client certificates here");
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("no client certificates here");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
