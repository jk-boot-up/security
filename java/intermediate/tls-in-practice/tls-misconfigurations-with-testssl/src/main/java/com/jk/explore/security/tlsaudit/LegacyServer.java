package com.jk.explore.security.tlsaudit;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** A server that speaks what old clients speak: TLS 1.0 and 1.1 next to 1.2, and only RSA key exchange, so no forward secrecy. The JDK refuses all of that unless it is told otherwise. */
final class LegacyServer implements AutoCloseable {

    private final SSLServerSocket socket;
    private final Thread thread;

    LegacyServer(Path pkcs12) throws IOException, GeneralSecurityException {
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(pkcs12)) {
            store.load(in, "changeit".toCharArray());
        }
        KeyManagerFactory keys = KeyManagerFactory.getInstance("SunX509");
        keys.init(store, "changeit".toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), null, null);
        socket = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(0, 50, InetAddress.getLoopbackAddress());
        socket.setEnabledProtocols(new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"});
        List<String> suites = new ArrayList<>();
        for (String suite : socket.getSupportedCipherSuites()) {
            if (suite.startsWith("TLS_RSA_WITH_") && !suite.contains("NULL")) {
                suites.add(suite);
            }
        }
        socket.setEnabledCipherSuites(suites.toArray(String[]::new));
        thread = Thread.ofPlatform().daemon().start(this::serve);
    }

    int port() {
        return socket.getLocalPort();
    }

    private void serve() {
        while (!socket.isClosed()) {
            try (SSLSocket client = (SSLSocket) socket.accept()) {
                client.setSoTimeout(3000);
                client.getInputStream().read(new byte[4096]);
                client.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 8\r\nConnection: close\r\n\r\nthe shop".getBytes());
            } catch (IOException e) {
                // a scan hangs up in every way it can
            }
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
