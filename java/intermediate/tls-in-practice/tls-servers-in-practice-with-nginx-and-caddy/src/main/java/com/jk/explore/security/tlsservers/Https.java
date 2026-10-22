package com.jk.explore.security.tlsservers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

/** A small HTTPS client over a socket: the address is 127.0.0.1, the name is whatever the test says, so names under .localhost need no resolver. */
final class Https {

    /** What came back, or why the handshake was refused. */
    record Response(int status, Map<String, String> headers, String body, String protocol, int stapled, List<X509Certificate> chain, String refusal) {

        boolean refused() {
            return !refusal.isEmpty();
        }

        String header(String name) {
            return headers.get(name.toLowerCase());
        }

        X509Certificate leaf() {
            return chain.get(0);
        }
    }

    private Https() {
    }

    static SSLContext trusting(Path... roots) throws IOException, GeneralSecurityException {
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null, null);
        int i = 0;
        for (Path root : roots) {
            store.setCertificateEntry("root" + i++, certificates(root).get(0));
        }
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, factory.getTrustManagers(), null);
        return context;
    }

    static List<X509Certificate> certificates(Path pem) throws IOException, GeneralSecurityException {
        try (InputStream in = Files.newInputStream(pem)) {
            return CertificateFactory.getInstance("X.509").generateCertificates(in).stream().map(c -> (X509Certificate) c).toList();
        }
    }

    /** GET https://{@code name}:{@code port}{@code path} with the name checked against the certificate; {@code protocols} limits what the client offers. */
    static Response get(int port, String name, String path, SSLContext context, String... protocols) throws IOException {
        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(10_000);
            SSLParameters p = socket.getSSLParameters();
            p.setServerNames(List.of(new SNIHostName(name)));
            p.setEndpointIdentificationAlgorithm("HTTPS");
            p.setApplicationProtocols(new String[] {"http/1.1"});
            if (protocols.length > 0) {
                p.setProtocols(protocols);
            }
            socket.setSSLParameters(p);
            try {
                socket.startHandshake();
            } catch (SSLException e) {
                return new Response(0, Map.of(), "", "", 0, List.of(), refusal(e));
            }
            ExtendedSSLSession session = (ExtendedSSLSession) socket.getSession();
            List<X509Certificate> chain = new ArrayList<>();
            try {
                for (java.security.cert.Certificate c : session.getPeerCertificates()) {
                    chain.add((X509Certificate) c);
                }
            } catch (SSLException e) {
                return new Response(0, Map.of(), "", "", 0, List.of(), refusal(e));
            }
            socket.getOutputStream().write(("GET " + path + " HTTP/1.1\r\nHost: " + name + ":" + port + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            Response response = parse(socket.getInputStream(), session.getProtocol(), session.getStatusResponses().size(), chain);
            // the JDK keeps sessions by address and port, not by name: a resumed session would skip the name check for the next name asked on the same port
            session.invalidate();
            return response;
        }
    }

    /** GET over plain http. */
    static Response plain(int port, String name, String path) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(10_000);
            socket.getOutputStream().write(("GET " + path + " HTTP/1.1\r\nHost: " + name + ":" + port + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            return parse(socket.getInputStream(), "none", 0, List.of());
        }
    }

    private static Response parse(InputStream in, String protocol, int stapled, List<X509Certificate> chain) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        in.transferTo(bytes);
        String text = bytes.toString(StandardCharsets.ISO_8859_1);
        int split = text.indexOf("\r\n\r\n");
        String[] lines = text.substring(0, split).split("\r\n");
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            headers.put(lines[i].substring(0, colon).toLowerCase(), lines[i].substring(colon + 1).strip());
        }
        return new Response(Integer.parseInt(lines[0].split(" ")[1]), headers, text.substring(split + 4).strip(), protocol, stapled, chain, "");
    }

    /** The JDK's long exception text, in a short sentence. */
    private static String refusal(SSLException e) {
        String text = String.valueOf(e.getMessage()) + (e.getCause() == null ? "" : " " + e.getCause().getMessage());
        if (text.contains("PKIX path building failed")) {
            return "the certificate is not signed by anything this client trusts";
        }
        if (text.contains("No subject alternative") || text.contains("No name matching")) {
            return "the certificate is not for that name";
        }
        if (text.contains("protocol_version") || text.contains("No appropriate protocol") || text.contains("handshake_failure") || text.contains("Received fatal alert")) {
            return "no protocol version in common";
        }
        return "handshake failed";
    }
}
