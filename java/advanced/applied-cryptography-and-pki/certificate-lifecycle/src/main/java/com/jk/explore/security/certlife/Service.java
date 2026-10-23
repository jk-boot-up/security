package com.jk.explore.security.certlife;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedKeyManager;

/** A TLS service on 127.0.0.1 whose certificate can be replaced while it runs: the next handshake uses the new one. */
final class Service implements AutoCloseable {

    private volatile Credential credential;
    private final SSLServerSocket server;
    private final AtomicInteger served = new AtomicInteger();

    Service(Credential first) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        this.credential = first;
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(new KeyManager[] {new Swappable()}, null, null);
        this.server = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread.ofVirtual().start(this::acceptLoop);
    }

    int port() {
        return server.getLocalPort();
    }

    /** Rotation: from now on new handshakes present this certificate. */
    void install(Credential next) {
        this.credential = next;
    }

    Credential credential() {
        return credential;
    }

    /** Handshakes that completed. */
    int served() {
        return served.get();
    }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                Socket s = server.accept();
                Thread.ofVirtual().start(() -> handle((SSLSocket) s));
            } catch (IOException closed) {
                return;
            }
        }
    }

    private void handle(SSLSocket socket) {
        try (socket) {
            socket.setSoTimeout(5000);
            socket.startHandshake();
            socket.getOutputStream().write('k');
            served.incrementAndGet();
        } catch (IOException refusedByTheClient) {
            // the client closed the connection or did not trust the certificate
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
    }

    private final class Swappable extends X509ExtendedKeyManager {

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return new String[] {"service"};
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return "service";
        }

        @Override
        public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
            return "service";
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return credential.chain().toArray(new X509Certificate[0]);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return credential.key();
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return null;
        }
    }
}
