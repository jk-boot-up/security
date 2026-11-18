package com.jk.explore.security.tlscapture;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Five demos: what a capture of a login shows, what a key log is, what it opens, what it does not, and what a leaked server key opens. */
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
            act2(out, pcap, keys);
            act3(out, pcap, keys);
            Path pcap2 = work.resolve("login2.pcap");
            Path keys2 = work.resolve("keys2.log");
            login(modern.port(), ca, "-tls1_3", pcap2, keys2);
            act4(out, work, pcap, keys, pcap2, keys2);
            Path tls12 = work.resolve("tls12.pcap");
            login(rsa.port(), ca, "-tls1_2", tls12, null);
            Path rsaKex = work.resolve("rsakex.pcap");
            try (Tap tap = new Tap(legacy.port(), rsaKex)) {
                Client.rsaLogin(tap.port(), ca.root());
                tap.awaitCapture();
            }
            act5(out, pcap, tls12, rsaKex, ca);
        }
        return out.toString();
    }

    private static void head(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void line(StringBuilder out, String text) {
        out.append("  ").append(text).append('\n');
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    /** One login through a tap, which writes {@code pcap}; the client writes its secrets to {@code keys} when that is not null. */
    private static void login(int serverPort, LocalCa ca, String protocol, Path pcap, Path keys) throws Exception {
        try (Tap tap = new Tap(serverPort, pcap)) {
            Client.login(tap.port(), ca.root(), protocol, keys);
            tap.awaitCapture();
        }
    }

    private static String suite(Path pcap) throws Exception {
        return Suites.name(Tshark.fields(pcap, "tls.handshake.type == 2", List.of(), "tls.handshake.ciphersuite").get(0).get(0));
    }

    private static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

    private static void act1(StringBuilder out, Path pcap) throws Exception {
        head(out, "ONE. What the wire shows.");
        head(out, "THE LOGIN (what the client sends, inside TLS 1.3):");
        line(out, "POST /login  Host: " + HOST);
        line(out, Client.BODY);
        head(out, "THE CAPTURE, READ WITH NO SECRETS (tshark -r login.pcap):");
        String name = Tshark.fields(pcap, "tls.handshake.type == 1", List.of(), "tls.handshake.extensions_server_name").get(0).get(0);
        line(out, "Client Hello  server name " + name);
        line(out, "Server Hello  " + suite(pcap));
        line(out, "certificate   " + (Tshark.count(pcap, "tls.handshake.type == 11", List.of()) > 0 ? "visible" : "not visible"));
        line(out, "HTTP          " + Tshark.count(pcap, "http", List.of()) + " requests readable");
        boolean word = new String(Files.readAllBytes(pcap), StandardCharsets.ISO_8859_1).contains("password");
        line(out, "the word password in the file's bytes: " + yesNo(word));
        head(out, "THE CLIENT HELLO, BY HAND (the first record's bytes):");
        ClientHello hello = ClientHello.read(pcap);
        line(out, "record type   " + hello.recordType() + " (handshake)");
        line(out, "message type  " + hello.messageType() + " (Client Hello)");
        line(out, "random        " + hello.random().length() / 2 + " bytes, new for every connection");
        line(out, "server_name   " + hello.serverName() + "  <- IN THE CLEAR");
        head(out, "THE REQUEST'S SIZE, WORKED OUT (what a watcher still sees):");
        int request = Client.REQUEST.length();
        line(out, "request " + request + " + type 1 + tag 16 = " + (request + 1 + 16) + " bytes");
        line(out, "on the wire (tshark)  " + requestRecord(pcap) + " bytes");
    }

    /** The largest encrypted record the client sent: the login request. */
    private static int requestRecord(Path pcap) throws Exception {
        String client = Tshark.fields(pcap, "tls.handshake.type == 1", List.of(), "tcp.srcport").get(0).get(0);
        int largest = 0;
        for (List<String> row : Tshark.fields(pcap, "tcp.srcport == " + client + " && tls.app_data", List.of(), "tls.record.length")) {
            for (String length : row.get(0).split(",")) {
                largest = Math.max(largest, Integer.parseInt(length));
            }
        }
        return largest;
    }

    private static void act2(StringBuilder out, Path pcap, Path keys) throws Exception {
        head(out, "TWO. A key log: the secrets of one connection.");
        KeyLog log = KeyLog.read(keys);
        head(out, "THE INPUT (openssl s_client -keylogfile keys.log):");
        line(out, log.lines().size() + " lines, as a browser writes with SSLKEYLOGFILE set");
        head(out, "THE KEY LOG (each label, and what its secret opens):");
        KeyLog.OPENS.forEach((label, opens) -> {
            if (log.lines().stream().anyMatch(l -> l.label().equals(label))) {
                line(out, pad(label, 33) + opens);
            }
        });
        head(out, "ONE LINE, TAKEN APART:");
        KeyLog.Line one = log.line("CLIENT_TRAFFIC_SECRET_0");
        line(out, "label          " + one.label());
        line(out, "client random  " + one.clientRandom().length() / 2 + " bytes (" + one.clientRandom().length() + " hex digits)");
        line(out, "secret         " + one.secret().length() / 2 + " bytes (" + one.secret().length() + " hex digits)");
        head(out, "THE SECRET'S SIZE, WORKED OUT (from the suite's hash):");
        String suite = suite(pcap);
        String hash = Suites.hash(suite);
        line(out, suite + " -> " + hash + " -> " + hash.substring(3) + " / 8 = " + Suites.secretBytes(suite) + " bytes");
        head(out, "THE MATCH, BY HAND (whose connection is it?):");
        line(out, "one client random in all lines  " + yesNo(log.lines().stream().map(KeyLog.Line::clientRandom).distinct().count() == 1));
        line(out, "the Client Hello's random       " + yesNo(log.opens(ClientHello.read(pcap).random())));
        line(out, "a private key in the file       " + yesNo(Files.readString(keys).contains("PRIVATE KEY")));
    }

    private static void act3(StringBuilder out, Path pcap, Path keys) throws Exception {
        head(out, "THREE. The same capture, read with the key log.");
        List<String> keyLog = Tshark.keyLog(keys);
        head(out, "THE INPUT (tshark -o tls.keylog_file:keys.log):");
        line(out, "the same login.pcap, and the lines of keys.log");
        head(out, "THE REQUEST (opened with CLIENT_TRAFFIC_SECRET_0):");
        List<String> request = Tshark.fields(pcap, "http.request", keyLog, "http.request.method", "http.request.uri", "urlencoded-form.key", "urlencoded-form.value").get(0);
        line(out, request.get(0) + " " + request.get(1));
        String[] names = request.get(2).split(",");
        String[] values = request.get(3).split(",");
        for (int i = 0; i < names.length; i++) {
            line(out, pad(names[i], 9) + "= " + values[i]);
        }
        head(out, "THE RESPONSE (opened with SERVER_TRAFFIC_SECRET_0):");
        List<String> response = Tshark.fields(pcap, "http.response", keyLog, "http.response.code", "http.set_cookie", "http.file_data").get(0);
        line(out, response.get(0) + "  " + Tshark.text(response.get(2)));
        line(out, "Set-Cookie: " + response.get(1));
        head(out, "THE CERTIFICATE (opened with SERVER_HANDSHAKE_TRAFFIC_SECRET):");
        List<String> cert = Tshark.fields(pcap, "tls.handshake.type == 11", keyLog, "x509ce.dNSName", "x509sat.printableString").get(0);
        line(out, "for        " + cert.get(0).split(",")[0] + "  <- ENCRYPTED IN TLS 1.3");
        line(out, "issued by  " + cert.get(1).split(",")[0]);
    }

    private static void act4(StringBuilder out, Path work, Path pcap, Path keys, Path pcap2, Path keys2) throws Exception {
        head(out, "FOUR. A key log opens its own connection and no other.");
        KeyLog log1 = KeyLog.read(keys);
        KeyLog log2 = KeyLog.read(keys2);
        KeyLog both = log1.plus(log2);
        Path bothFile = work.resolve("both.log");
        Files.writeString(bothFile, both.text());
        String random2 = ClientHello.read(pcap2).random();
        head(out, "THE INPUT (a second login, with its own key log):");
        line(out, "login.pcap   keys.log    the first login");
        line(out, "login2.pcap  keys2.log   the second login");
        head(out, "THE MATCH, BY HAND (the log has login2's client random?):");
        line(out, "keys.log   " + yesNo(log1.opens(random2)));
        line(out, "keys2.log  " + yesNo(log2.opens(random2)));
        line(out, "both.log   " + yesNo(both.opens(random2)) + "  (keys.log and keys2.log in one file)");
        head(out, "THE RESULT (tshark on login2.pcap):");
        line(out, "keys.log   " + opened(Tshark.count(pcap2, "http", Tshark.keyLog(keys))));
        line(out, "keys2.log  " + opened(Tshark.count(pcap2, "http", Tshark.keyLog(keys2))));
        line(out, "both.log   " + opened(Tshark.count(pcap2, "http", Tshark.keyLog(bothFile))) + ", and login.pcap: "
                + opened(Tshark.count(pcap, "http", Tshark.keyLog(bothFile))).split(":")[0]);
        head(out, "BEFORE (all the lines of keys.log, on login.pcap):");
        sides(out, pcap, keys);
        Path clientOnly = work.resolve("client-only.log");
        Files.writeString(clientOnly, log1.only("CLIENT_").text());
        head(out, "AFTER (only its two CLIENT_ lines, on login.pcap):");
        sides(out, pcap, clientOnly);
    }

    private static String opened(int httpPackets) {
        return httpPackets > 0 ? "opened" : "closed: " + httpPackets + " HTTP packets";
    }

    private static void sides(StringBuilder out, Path pcap, Path keys) throws Exception {
        boolean requests = Tshark.count(pcap, "http.request", Tshark.keyLog(keys)) > 0;
        boolean responses = Tshark.count(pcap, "http.response", Tshark.keyLog(keys)) > 0;
        line(out, "requests " + (requests ? "opened" : "closed") + ", responses " + (responses ? "opened" : "closed") + (responses ? "" : "  <- ONE SECRET PER SIDE"));
    }

    private static void act5(StringBuilder out, Path tls13, Path tls12, Path rsaKex, LocalCa ca) throws Exception {
        head(out, "FIVE. The server's own key is a different matter.");
        List<String> who = List.of("TLS 1.3, nginx", "TLS 1.2, nginx", "TLS 1.2, old");
        List<Path> recordings = List.of(tls13, tls12, rsaKex);
        head(out, "THE RECORDINGS (the suite each Server Hello chose):");
        for (int i = 0; i < 3; i++) {
            line(out, pad(who.get(i), 16) + suite(recordings.get(i)));
        }
        head(out, "THE CERTIFICATE, WITH NO SECRETS:");
        line(out, "TLS 1.3  " + (Tshark.count(tls13, "tls.handshake.type == 11", List.of()) > 0 ? "visible" : "not visible"));
        line(out, "TLS 1.2  " + (Tshark.count(tls12, "tls.handshake.type == 11", List.of()) > 0 ? "visible" : "not visible"));
        head(out, "THE KEY EXCHANGE, BY HAND (read from the suite's name):");
        for (int i = 0; i < 3; i++) {
            line(out, pad(who.get(i), 16) + Suites.keyExchange(suite(recordings.get(i))));
        }
        head(out, "THE RULE, BY HAND (did the secret cross the wire?):");
        for (int i = 1; i < 3; i++) {
            line(out, pad(who.get(i), 16) + (Suites.serverKeyOpens(suite(recordings.get(i))) ? "yes, so the key opens it" : "no, so the key opens nothing"));
        }
        head(out, "THE LEAK (tshark with the server's private key, rsa.key):");
        List<String> serverKey = Tshark.serverKey(ca.key("rsa"));
        for (int i = 1; i < 3; i++) {
            List<List<String>> form = Tshark.fields(recordings.get(i), "http.request", serverKey, "urlencoded-form.value");
            String result = form.isEmpty() ? "closed: 0 HTTP packets" : "opened: password " + form.get(0).get(0).split(",")[1] + "  <- THE PAST IS OPEN";
            line(out, pad(who.get(i), 16) + result);
        }
    }
}
