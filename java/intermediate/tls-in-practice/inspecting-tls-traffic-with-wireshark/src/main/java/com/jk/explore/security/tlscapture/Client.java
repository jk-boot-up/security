package com.jk.explore.security.tlscapture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** The openssl command as a TLS client; with a key log file it writes the secrets of the connection, as a browser does when SSLKEYLOGFILE is set. */
final class Client {

    static final String BODY = "user=ana&password=hunter2";

    /** The whole login request, as the shopper's client writes it inside TLS. */
    static final String REQUEST = "POST /login HTTP/1.1\r\nHost: www.shop.localhost\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: " + BODY.length()
            + "\r\nConnection: close\r\n\r\n" + BODY;

    private Client() {
    }

    /** Logs in through {@code port} (nginx, or a tap in front of it) and returns the page that came back. */
    static String login(int port, Path root, String protocol, Path keyLog) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("openssl", "s_client", "-connect", "127.0.0.1:" + port, "-servername", "www.shop.localhost", "-verify_hostname", "www.shop.localhost",
                "-CAfile", root.toString(), "-quiet", "-ign_eof", protocol));
        if (keyLog != null) {
            command.addAll(List.of("-keylogfile", keyLog.toString()));
        }
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        p.getOutputStream().write(REQUEST.getBytes());
        p.getOutputStream().flush();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
        }
        return out;
    }

    /** The same login from a JDK client that offers only RSA key exchange (TLS_RSA_WITH_AES_128_CBC_SHA), for the old-style server. */
    static void rsaLogin(int port, Path root) throws IOException, java.security.GeneralSecurityException {
        java.security.KeyStore store = java.security.KeyStore.getInstance("PKCS12");
        store.load(null, null);
        try (var in = java.nio.file.Files.newInputStream(root)) {
            store.setCertificateEntry("root", java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(in));
        }
        javax.net.ssl.TrustManagerFactory trust = javax.net.ssl.TrustManagerFactory.getInstance("PKIX");
        trust.init(store);
        javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");
        context.init(null, trust.getTrustManagers(), null);
        try (javax.net.ssl.SSLSocket socket = (javax.net.ssl.SSLSocket) context.getSocketFactory().createSocket(java.net.InetAddress.getLoopbackAddress(), port)) {
            socket.setEnabledProtocols(new String[] {"TLSv1.2"});
            String[] rsaOnly = {"TLS_RSA_WITH_AES_128_CBC_SHA"};
            socket.setEnabledCipherSuites(rsaOnly);
            socket.startHandshake();
            socket.getOutputStream().write(REQUEST.getBytes());
            socket.getInputStream().readAllBytes();
        }
    }
}
