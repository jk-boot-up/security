package com.jk.explore.security.tlscapture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TlsCaptureDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static final String HOST = "www.shop.localhost";

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Path work = Files.createTempDirectory("tlscapture");
        LocalCa ca = new LocalCa(work.resolve("ca"));
        ca.issue("www", "-1h", "2160h", false, HOST);
        ca.issue("rsa", "-1h", "2160h", true, HOST);
        try (Nginx modern = new Nginx(work.resolve("modern")); Nginx rsa = new Nginx(work.resolve("rsa")); LegacyServer legacy = new LegacyServer(ca.pkcs12("rsa"))) {
            modern.start("TLSv1.3", ca.cert("www").toString(), ca.key("www").toString());
            rsa.start("TLSv1.2", ca.cert("rsa").toString(), ca.key("rsa").toString());
            Path pcap = work.resolve("login.pcap");
            Path keys = work.resolve("keys.log");
            login(modern.port(), ca, "-tls1_3", pcap, keys);
            act1(out, pcap);
            act2(out, keys);
            act3(out, pcap, keys);
            Path pcap2 = work.resolve("login2.pcap");
            Path keys2 = work.resolve("keys2.log");
            login(modern.port(), ca, "-tls1_3", pcap2, keys2);
            act4(out, work, pcap, keys, pcap2, keys2);
            Path tls12 = work.resolve("tls12.pcap");
            login(rsa.port(), ca, "-tls1_2", tls12, null);
            Path rsaKex = work.resolve("rsakex.pcap");
            legacyLogin(legacy.port(), ca, rsaKex);
            act5(out, pcap, tls12, rsaKex, rsa.port(), legacy.port(), ca);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** One login through a tap, which writes {@code pcap}; the client writes its secrets to {@code keys} when that is not null. */
    private static String login(int serverPort, LocalCa ca, String protocol, Path pcap, Path keys) throws Exception {
        try (Tap tap = new Tap(serverPort, pcap)) {
            String page = Client.login(tap.port(), ca.root(), protocol, keys);
            tap.awaitCapture();
            return page;
        }
    }

    private static void legacyLogin(int serverPort, LocalCa ca, Path pcap) throws Exception {
        try (Tap tap = new Tap(serverPort, pcap)) {
            javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");
            javax.net.ssl.TrustManagerFactory trust = javax.net.ssl.TrustManagerFactory.getInstance("PKIX");
            java.security.KeyStore store = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
            store.load(null, null);
            try (var in = Files.newInputStream(ca.root())) {
                store.setCertificateEntry("root", java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(in));
            }
            trust.init(store);
            context.init(null, trust.getTrustManagers(), null);
            try (javax.net.ssl.SSLSocket socket = (javax.net.ssl.SSLSocket) context.getSocketFactory().createSocket(java.net.InetAddress.getLoopbackAddress(), tap.port())) {
                socket.setEnabledProtocols(new String[] {"TLSv1.2"});
                socket.setEnabledCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA"});
                socket.startHandshake();
                socket.getOutputStream().write(("POST /login HTTP/1.1\r\nHost: " + HOST + "\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: " + Client.BODY.length() + "\r\nConnection: close\r\n\r\n" + Client.BODY).getBytes());
                socket.getInputStream().readAllBytes();
            }
            tap.awaitCapture();
        }
    }

    private static void act1(StringBuilder out, Path pcap) throws Exception {
        line(out, "ONE. What the wire shows.");
        line(out, "  a login to the shop over TLS 1.3, copied by a tap that sees only what a machine on the path sees, then read with tshark and no secrets");
        List<List<String>> hello = Tshark.fields(pcap, "tls.handshake.type == 1", List.of(), "tls.handshake.extensions_server_name");
        line(out, "  the Client Hello names the shop in the clear: " + hello.get(0).get(0));
        line(out, "  the Server Hello chooses: " + Tshark.fields(pcap, "tls.handshake.type == 2", List.of(), "tls.handshake.ciphersuite").get(0).get(0).replace("0x1302", "TLS_AES_256_GCM_SHA384").replace("0x1301", "TLS_AES_128_GCM_SHA256"));
        line(out, "  the certificate is in the capture: " + (Tshark.count(pcap, "tls.handshake.type == 11", List.of()) > 0) + "; HTTP requests tshark can read: " + Tshark.count(pcap, "http", List.of()) + "; encrypted application data records: " + (Tshark.count(pcap, "tls.app_data", List.of()) > 0));
        line(out, "  the bytes of the capture contain the word password: " + new String(Files.readAllBytes(pcap), java.nio.charset.StandardCharsets.ISO_8859_1).contains("password") + "  <- THE PASSWORD IS THERE ONLY AS NOISE");
        line(out, "  what a watcher learns anyway: the name, the times and the sizes");
    }

    private static void act2(StringBuilder out, Path keys) throws Exception {
        line(out, "TWO. A key log: the secrets of one connection.");
        List<String> lines = Files.readAllLines(keys).stream().filter(l -> !l.startsWith("#")).toList();
        line(out, "  openssl s_client -keylogfile wrote " + lines.size() + " lines, as a browser does when SSLKEYLOGFILE is set: " + lines.stream().map(l -> l.split(" ")[0]).sorted().toList());
        line(out, "  each line is a label, the connection's client random (" + lines.get(0).split(" ")[1].length() / 2 + " bytes) and a secret (" + lines.get(0).split(" ")[2].length() / 2 + " bytes), which ties it to one connection and nothing else");
        line(out, "  the file holds no private key: " + !String.join("", lines).contains("BEGIN"));
    }

    private static void act3(StringBuilder out, Path pcap, Path keys) throws Exception {
        line(out, "THREE. The same capture, read with the key log.");
        List<String> keyLog = Tshark.keyLog(keys);
        List<List<String>> request = Tshark.fields(pcap, "http.request", keyLog, "http.request.method", "http.request.uri", "urlencoded-form.key", "urlencoded-form.value");
        line(out, "  request: " + request.get(0).get(0) + " " + request.get(0).get(1) + ", form fields " + request.get(0).get(2) + " = " + request.get(0).get(3));
        List<List<String>> response = Tshark.fields(pcap, "http.response", keyLog, "http.response.code", "http.set_cookie", "http.file_data");
        line(out, "  response: " + response.get(0).get(0) + ", " + response.get(0).get(1) + ", " + Tshark.text(response.get(0).get(2)));
        line(out, "  the encrypted handshake opens too: the certificate is for " + Tshark.fields(pcap, "tls.handshake.type == 11", keyLog, "x509ce.dNSName").get(0).get(0).split(",")[0] + "  <- IN TLS 1.3 THE CERTIFICATE IS ENCRYPTED, AND THE KEY LOG OPENS IT");
    }

    private static void act4(StringBuilder out, Path work, Path pcap, Path keys, Path pcap2, Path keys2) throws Exception {
        line(out, "FOUR. A key log opens its own connection and no other.");
        line(out, "  a second login, read with the first login's key log: " + Tshark.count(pcap2, "http", Tshark.keyLog(keys)) + " HTTP packets; with its own key log: " + (Tshark.count(pcap2, "http", Tshark.keyLog(keys2)) > 0 ? "opened" : "closed"));
        Path both = work.resolve("both.log");
        Files.writeString(both, Files.readString(keys) + Files.readString(keys2));
        line(out, "  one file with both logs opens both captures: " + (Tshark.count(pcap, "http", Tshark.keyLog(both)) > 0 && Tshark.count(pcap2, "http", Tshark.keyLog(both)) > 0));
        Path clientOnly = work.resolve("client-only.log");
        Files.writeString(clientOnly, Files.readAllLines(keys).stream().filter(l -> l.startsWith("CLIENT_TRAFFIC_SECRET_0") || l.startsWith("CLIENT_HANDSHAKE")).reduce("", (a, b) -> a + b + "\n"));
        line(out, "  only the client's lines: requests opened: " + (Tshark.count(pcap, "http.request", Tshark.keyLog(clientOnly)) > 0) + ", responses opened: " + (Tshark.count(pcap, "http.response", Tshark.keyLog(clientOnly)) > 0));
    }

    private static void act5(StringBuilder out, Path tls13, Path tls12, Path rsaKex, int rsaPort, int legacyPort, LocalCa ca) throws Exception {
        line(out, "FIVE. The server's own key is a different matter.");
        line(out, "  the same login over TLS 1.2: the certificate is visible without any secret: " + (Tshark.count(tls12, "tls.handshake.type == 11", List.of()) > 0) + "; over TLS 1.3 it is not: " + (Tshark.count(tls13, "tls.handshake.type == 11", List.of()) == 0));
        int ecdhe = Tshark.count(tls12, "http", Tshark.serverKey(ca.key("rsa")));
        line(out, "  recorded with ephemeral key exchange (ECDHE), then the server's private key leaks: HTTP packets opened: " + ecdhe);
        List<List<String>> opened = Tshark.fields(rsaKex, "http.request", Tshark.serverKey(ca.key("rsa")), "urlencoded-form.value");
        line(out, "  recorded with RSA key exchange, then the same kind of key leaks: " + (opened.isEmpty() ? "closed" : "opened, password " + opened.get(0).get(0).split(",")[1]) + "  <- A LEAKED KEY OPENS EVERYTHING RECORDED BEFORE");
        line(out, "  a key log is as good as the password it protects: where it is written, who can read it and when it is deleted are part of the bill");
        line(out, "  nothing here needed a network card: the tap wrote the capture, because reading a card needs rights that a test should not ask for");
    }
}
