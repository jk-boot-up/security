package com.jk.explore.security.tlsattacks;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

/** Deliberately weak local TLS servers, probed and scanned, and what TLS 1.3 removed. Everything runs on 127.0.0.1. */
public class TlsAttacksDemo {

    static final String SECRET_REQUEST = "POST /transfer?from=ACC-1001&to=ACC-2002&amount=2500";

    public static void main(String[] args) throws Exception {
        System.out.print(run(Path.of("build")));
    }

    static String yn(boolean b) {
        return b ? "yes" : "no";
    }

    static String names(Set<Weakness> set) {
        return set.isEmpty() ? "none" : String.join(", ", set.stream().map(Weakness::shortName).toList());
    }

    static Set<Weakness> missed(Set<Weakness> found) {
        Set<Weakness> m = EnumSet.copyOf(Weakness.PLANTED);
        m.removeAll(found);
        return m;
    }

    /** A key left-aligned in a column of the given width, then the value. */
    static String col(String key, int width, String value) {
        return key + " ".repeat(Math.max(1, width - key.length())) + value;
    }

    /** The printed output: an act heading, section headers indented by two, lines by four. */
    static final class Out {
        private final StringBuilder text = new StringBuilder();

        void act(String heading) {
            text.append(heading).append('\n');
        }

        void section(String header) {
            text.append("  ").append(header).append('\n');
        }

        void line(String line) {
            text.append("    ").append(line).append('\n');
        }

        @Override
        public String toString() {
            return text.toString();
        }
    }

    public static String run(Path buildDir) throws Exception {
        Out out = new Out();
        Openssl openssl = Openssl.find();
        Path testssl = TestSsl.install(buildDir);
        try (Lab lab = new Lab(openssl);
                Server weak = new Server(lab, Server.Profile.WEAK);
                Server strong = new Server(lab, Server.Profile.STRONG);
                Server only13 = new Server(lab, Server.Profile.TLS13_ONLY)) {
            CompletableFuture<List<TestSsl.Finding>> weakScan = scanAsync(testssl, openssl, lab, "weak.json", weak.port());
            CompletableFuture<List<TestSsl.Finding>> strongScan = scanAsync(testssl, openssl, lab, "strong.json", strong.port());
            lab(out, openssl, weak.port(), strong.port(), only13.port());
            scored(out, openssl, weak.port(), strong.port(), weakScan.get(), strongScan.get());
            downgrade(out, openssl, weak.port());
            forwardSecrecy(out, openssl, lab, weak.port());
            tls13(out, openssl, weak.port(), only13.port());
        }
        return out.toString();
    }

    private static CompletableFuture<List<TestSsl.Finding>> scanAsync(Path script, Openssl openssl, Lab lab, String file, int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return TestSsl.scan(script, openssl, lab.scratch(file), port);
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    // ------------------------------------------------------------------ ONE: the lab

    static void lab(Out out, Openssl openssl, int w, int s, int t) throws Exception {
        out.act("ONE. The lab: a weak server and two controls.");
        out.section("THE LAB (three local servers on 127.0.0.1):");
        out.line(col("servers", 10, "weak, strong, tls 1.3 only"));
        out.line(col("made by", 10, "openssl s_server, OpenSSL " + Openssl.SERIES));
        out.line(col("scanner", 10, "testssl.sh " + TestSsl.TAG + " (commit " + TestSsl.COMMIT_PREFIX + ")"));
        out.section("THE WEAKNESSES PLANTED IN THE WEAK SERVER (" + Weakness.PLANTED.size() + "):");
        for (Weakness k : Weakness.PLANTED) {
            out.line((k.ordinal() + 1) + "  " + k.label());
        }
        out.section(col("WHAT EACH SERVER ACCEPTS:", 32, "weak  strong  1.3 only"));
        for (int i = 0; i < Audit.VERSIONS.length; i++) {
            out.line(cells(Audit.VERSION_NAMES[i], Audit.version(openssl, w, i).connected(),
                    Audit.version(openssl, s, i).connected(), Audit.version(openssl, t, i).connected()));
        }
        for (int i = 0; i < Audit.SUITES.length; i++) {
            out.line(cells(Audit.SUITES[i][0], Audit.suite(openssl, w, i).connected(),
                    Audit.suite(openssl, s, i).connected(), Audit.suite(openssl, t, i).connected()));
        }
        out.section("WHAT THE SUITE NAMES SAY (key exchange, cipher):");
        for (String[] suite : Audit.SUITES) {
            out.line(col(suite[0], 30, suite[1]));
        }
        out.section("HOW THE DH GROUP SIZE IS READ:");
        out.line(col("asked for", 10, Audit.SUITES[2][0] + ", weak server"));
        out.line(col("s_client", 10, "Peer Temp Key: DH, " + Audit.suite(openssl, w, 2).dhBits() + " bits"));
        out.line(col("rule", 10, "under 2048 bits is a weakness"));
    }

    static String cells(String what, boolean weak, boolean strong, boolean only13) {
        return col(what, 30, col(yn(weak), 6, col(yn(strong), 8, yn(only13))));
    }

    // ------------------------------------------------------------------ TWO: scored against the truth

    static void scored(Out out, Openssl openssl, int w, int s, List<TestSsl.Finding> weakReport, List<TestSsl.Finding> strongReport) throws Exception {
        out.act("TWO. Our probes and testssl.sh, scored against the truth.");
        Map<Weakness, String> probes = Audit.evidence(openssl, w);
        Set<Weakness> probesStrong = Audit.find(openssl, s);
        out.section("OUR PROBES ON THE WEAK SERVER (how each was found):");
        for (Weakness k : Weakness.PLANTED) {
            out.line(col(k.shortName(), 14, probes.getOrDefault(k, "missed")));
        }
        out.section("THE SCORE OF OUR PROBES:");
        out.line(col("weak", 8, "found " + probes.size() + " of " + Weakness.PLANTED.size() + ", missed: " + names(missed(probes.keySet()))));
        out.line(col("strong", 8, "found " + probesStrong.size() + ", false alarms: " + names(probesStrong)));
        Map<Weakness, String> report = TestSsl.evidence(weakReport);
        Set<Weakness> tsStrong = TestSsl.detect(strongReport);
        Set<Weakness> byTable = EnumSet.of(Weakness.STATIC_RSA, Weakness.SMALL_DH);
        byTable.retainAll(report.keySet());
        out.section("TESTSSL.SH " + TestSsl.TAG + " ON THE WEAK SERVER (where each shows):");
        for (Weakness k : Weakness.PLANTED) {
            out.line(col(k.shortName(), 14, report.getOrDefault(k, "missed")));
        }
        out.section("THE SCORE OF TESTSSL.SH:");
        out.line(col("weak", 8, "found " + report.size() + " of " + Weakness.PLANTED.size() + ", missed: " + names(missed(report.keySet()))));
        out.line(col("", 8, (report.size() - byTable.size()) + " by a finding, " + byTable.size() + " only in the cipher table"));
        out.line(col("strong", 8, "found " + tsStrong.size() + ", findings of LOW or worse: " + TestSsl.atLeastLow(strongReport)));
    }

    // ------------------------------------------------------------------ THREE: the downgrade dance

    /** One connection attempt of a client that retries: what it offered and what came back. */
    record Attempt(String offered, boolean connected, String result) {
    }

    /** What a client that retries ended with. */
    record Outcome(boolean connected, List<Attempt> attempts, String protocol, String cipher, String alert) {
    }

    static final String[] OFFERED = {"TLS 1.0", "TLS 1.1", "TLS 1.2", "up to TLS 1.3"};

    /** A client that retries with lower versions when a handshake fails, with or without the fallback signal, never below a minimum. */
    static Outcome dance(Openssl openssl, int port, boolean scsv, int minVersionIndex) throws IOException, InterruptedException {
        String[] versionFlags = {"-tls1", "-tls1_1", "-tls1_2", ""};
        List<Attempt> attempts = new ArrayList<>();
        String lastAlert = "";
        for (int i = 3; i >= minVersionIndex; i--) {
            List<String> flags = new ArrayList<>();
            if (!versionFlags[i].isEmpty()) {
                flags.add(versionFlags[i]);
            }
            flags.addAll(List.of("-cipher", "DEFAULT:@SECLEVEL=0"));
            boolean signal = scsv && !attempts.isEmpty();
            if (signal) {
                flags.add("-fallback_scsv");
            }
            Probe p = Probe.run(openssl, port, "", flags.toArray(String[]::new));
            String offered = OFFERED[i] + (signal ? " + SCSV" : "");
            if (p.connected()) {
                attempts.add(new Attempt(offered, true, "connected, " + p.cipher()));
                return new Outcome(true, attempts, p.protocol(), p.cipher(), "");
            }
            attempts.add(new Attempt(offered, false, p.alert().isEmpty() ? "reset by the network" : "refused: " + p.alert()));
            lastAlert = p.alert();
        }
        return new Outcome(false, attempts, "", "", lastAlert.isEmpty() ? "none, the connection was reset" : lastAlert);
    }

    static void attempts(Out out, Outcome o) {
        for (int i = 0; i < o.attempts().size(); i++) {
            Attempt a = o.attempts().get(i);
            out.line(col("attempt " + (i + 1), 11, col(a.offered(), 16, a.result())));
        }
    }

    static void downgrade(Out out, Openssl openssl, int weakPort) throws Exception {
        out.act("THREE. The downgrade dance.");
        try (Tap clean = new Tap(weakPort, 0xFFFF)) {
            Probe.run(openssl, clean.port(), "", "-tls1_2", "-cipher", "DEFAULT:@SECLEVEL=0");
            Probe.run(openssl, clean.port(), "");
            List<Tap.Capture> c = clean.await(2);
            out.section("THE SERVER HELLO RANDOM (a server that can speak TLS 1.3):");
            byte[] pushed = serverHello(c, 0);
            out.line(col("asked for TLS 1.2", 19, "last 8 bytes " + TlsRecords.randomTail(pushed)));
            out.line(col("", 19, "\"DOWNGRD\" + 01: " + sentinel(pushed)));
            out.line(col("asked for TLS 1.3", 19, sentinel(serverHello(c, 1))));
        }
        try (Tap breaker = new Tap(weakPort, 0x0301)) {
            out.section("THE NETWORK (a broken or hostile middlebox):");
            out.line("resets every hello that offers more than TLS 1.0");
            Outcome plain = dance(openssl, breaker.port(), false, 0);
            out.section("A CLIENT THAT FALLS BACK WITH NO SIGNAL:");
            attempts(out, plain);
            out.line(col("result", 11, "connected " + yn(plain.connected()) + ", on " + plain.protocol() + " with " + plain.cipher()));
            Outcome scsv = dance(openssl, breaker.port(), true, 0);
            out.section("THE SAME CLIENT, SENDING TLS_FALLBACK_SCSV ON EVERY RETRY:");
            attempts(out, scsv);
            out.line(col("result", 11, "connected " + yn(scsv.connected()) + ", the server saw the signal"));
            Outcome floor = dance(openssl, breaker.port(), false, 2);
            out.section("A CLIENT WITH A MINIMUM OF TLS 1.2:");
            attempts(out, floor);
            out.line(col("result", 11, "connected " + yn(floor.connected()) + ", it never offered TLS 1.0"));
            out.section("THE JDK CLIENT, DEFAULT SETTINGS, THROUGH THE SAME NETWORK:");
            out.line(col("enabled", 11, String.join(", ", SSLContext.getDefault().getDefaultSSLParameters().getProtocols())));
            out.line(col("result", 11, "handshake failed " + yn(!jdkConnects(breaker.port())) + ", retried older: no"));
        }
    }

    static byte[] serverHello(List<Tap.Capture> captures, int i) {
        return TlsRecords.message(TlsRecords.clearHandshake(TlsRecords.records(captures.get(i).serverToClient())), TlsRecords.SERVER_HELLO);
    }

    static String sentinel(byte[] hello) {
        if (hello == null) {
            return "no ServerHello";
        }
        String s = TlsRecords.downgradeSentinel(hello);
        return s.equals("none") ? "no DOWNGRD marker in the last 8 bytes" : "a downgrade to " + s;
    }

    static boolean jdkConnects(int port) {
        try (SSLSocket socket = (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(5000);
            socket.startHandshake();
            return true;
        } catch (IOException | NoSuchAlgorithmException failed) {
            return false;
        }
    }

    // ------------------------------------------------------------------ FOUR: forward secrecy

    static final String[][] RECORDED = {{"AES128-GCM-SHA256", "static RSA"}, {"DHE-RSA-AES128-GCM-SHA256", "DHE"}, {"ECDHE-RSA-AES128-GCM-SHA256", "ECDHE"}};

    static void forwardSecrecy(Out out, Openssl openssl, Lab lab, int weakPort) throws Exception {
        out.act("FOUR. A recorded session, and the server key leaks later.");
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair unrelated = g.generateKeyPair();
        List<String> recorded = new ArrayList<>();
        List<String> exchanged = new ArrayList<>();
        List<String> results = new ArrayList<>();
        Decryptor.Opened opened = null;
        int unrelatedOpened = 0;
        for (String[] suite : RECORDED) {
            try (Tap tap = new Tap(weakPort, 0xFFFF)) {
                Probe p = Probe.run(openssl, tap.port(), SECRET_REQUEST + "\n", "-tls1_2", "-no_ems", "-cipher", suite[0] + ":@SECLEVEL=0");
                Tap.Capture cap = tap.await(1).get(0);
                recorded.add(col(suite[0], 29, "negotiated " + yn(p.connected() && suite[0].equals(p.cipher()))
                        + ", in clear: " + yn(contains(cap.clientToServer(), SECRET_REQUEST))));
                byte[] cke = TlsRecords.message(TlsRecords.clearHandshake(TlsRecords.records(cap.clientToServer())), TlsRecords.CLIENT_KEY_EXCHANGE);
                int n = TlsRecords.keyExchangeValueLength(cke, suite[1].equals("ECDHE"));
                exchanged.add(col(suite[1], 12, n + " bytes: " + switch (suite[1]) {
                    case "static RSA" -> "the secret, encrypted to the key";
                    case "DHE" -> "a DH public value only";
                    default -> "an EC public value only";
                }));
                Optional<Decryptor.Opened> leaked = Decryptor.open(cap.clientToServer(), cap.serverToClient(), lab.keyPair().getPrivate());
                if (Decryptor.clientData(cap.clientToServer(), cap.serverToClient(), unrelated.getPrivate()).isPresent()) {
                    unrelatedOpened++;
                }
                results.add(col(suite[1], 12, leaked.isPresent() ? "opened" : "not opened"));
                if (leaked.isPresent()) {
                    opened = leaked.get();
                    results.add(col("", 12, leaked.get().text().lines().findFirst().orElse("")));
                }
            }
        }
        out.section("THE RECORDING (one session per suite, through a tap):");
        out.line(col("request", 29, "the same in each session:"));
        out.line(SECRET_REQUEST);
        recorded.forEach(out::line);
        out.section("WHAT THE CLIENT SENT IN ITS KEY EXCHANGE MESSAGE:");
        exchanged.forEach(out::line);
        if (opened != null) {
            out.section("HOW THE LEAKED KEY OPENS THE STATIC RSA SESSION:");
            out.line("1 RSA-decrypt the " + opened.encryptedSecretBytes() + " bytes -> pre-master secret, " + opened.premasterBytes() + " bytes");
            out.line("2 PRF(pre-master, \"master secret\", randoms) -> " + opened.masterBytes() + " bytes");
            out.line("3 PRF(master, \"key expansion\", randoms)   -> " + opened.keyBlockBytes() + " bytes");
            out.line("4 key block bytes 0-15  -> client write key, " + opened.clientKeyBytes() + " bytes");
            out.line("5 key block bytes 32-35 -> client salt, " + opened.saltBytes() + " bytes");
            out.line("6 AES-GCM, nonce = salt + 8 bytes of the record -> the request");
        }
        out.section("THE RESULT, WITH THE LEAKED SERVER KEY:");
        results.forEach(out::line);
        out.line(col("unrelated", 12, "a key that is not the server's opened " + unrelatedOpened + " of " + RECORDED.length));
    }

    static boolean contains(byte[] haystack, String needle) {
        return new String(haystack, StandardCharsets.ISO_8859_1).contains(needle);
    }

    // ------------------------------------------------------------------ FIVE: TLS 1.3

    static void tls13(Out out, Openssl openssl, int weakPort, int only13Port) throws Exception {
        out.act("FIVE. What TLS 1.3 removed.");
        out.section("THE TLS 1.3 ONLY SERVER (its configuration):");
        out.line(col("minimum", 10, "TLS 1.3"));
        out.line(col("its file", 10, "still lists DHE-RSA-AES128-SHA, AES128-GCM-SHA256"));
        out.line(col("", 10, "and a 1024-bit DH group"));
        int refused = 0;
        String alert = "";
        for (String v : new String[] {"-tls1", "-tls1_1", "-tls1_2"}) {
            Probe p = Probe.with(openssl, only13Port, v, "DEFAULT:AES128-GCM-SHA256");
            if (!p.connected()) {
                refused++;
                alert = p.alert();
            }
        }
        Probe rsa = Probe.with(openssl, only13Port, "-tls1_2", "AES128-GCM-SHA256");
        Probe cbc = Probe.with(openssl, only13Port, "-tls1_2", "AES128-SHA");
        List<Integer> methods;
        try (Tap tap = new Tap(only13Port, 0xFFFF)) {
            Probe.run(openssl, tap.port(), "", "-tls1_3", "-comp");
            byte[] hello = TlsRecords.message(TlsRecords.clearHandshake(TlsRecords.records(tap.await(1).get(0).clientToServer())), TlsRecords.CLIENT_HELLO);
            methods = TlsRecords.compressionMethods(hello);
        }
        Probe dh = Probe.run(openssl, only13Port, "", "-tls1_3");
        out.section("WHAT A CLIENT ASKED FOR, AND THE ANSWER:");
        out.line(col("TLS 1.0, 1.1, 1.2", 20, "refused " + refused + " of 3: " + alert));
        out.line(col("static RSA", 20, "AES128-GCM-SHA256 connected " + yn(rsa.connected())));
        out.line(col("CBC with a MAC", 20, "AES128-SHA connected " + yn(cbc.connected())));
        out.line(col("compression", 20, "the hello offers methods " + methods + " (0 is none)"));
        out.line(col("renegotiation", 20, renegotiation(openssl, only13Port)));
        out.line(col("1024-bit DH group", 20, "used " + yn(dh.dhBits() == 1024) + ", group " + (dh.group().isEmpty() ? "none" : dh.group())));
        List<String[]> suites = List.of(openssl.run("", "ciphers", "-v", "-tls1_3").output().split("\n")).stream()
                .filter(l -> l.contains("TLSv1.3")).map(l -> l.trim().split("\\s+")).toList();
        out.section("THE TLS 1.3 SUITES (openssl ciphers -v -tls1_3):");
        for (String[] t : suites) {
            out.line(col(t[0], 30, field(t, "Kx") + " " + field(t, "Au") + " " + field(t, "Mac")));
        }
        long fixedKx = suites.stream().filter(t -> !field(t, "Kx").equals("Kx=any")).count();
        long notAead = suites.stream().filter(t -> !field(t, "Mac").equals("Mac=AEAD")).count();
        out.line(col("fixed key exchange", 20, fixedKx + " of " + suites.size() + "; not AEAD: " + notAead + " of " + suites.size()));
        out.section("THE WEAK SERVER ON TLS 1.2, FOR COMPARISON:");
        out.line(col("compression", 15, compression(openssl, weakPort)));
        out.line(col("renegotiation", 15, renegotiation(openssl, weakPort, "-tls1_2")));
        boolean rc4 = !openssl.run("", "ciphers", "RC4").output().contains("no cipher match");
        out.line(col("RC4", 15, rc4 ? "in this OpenSSL build" : "not in this OpenSSL build, so not planted"));
    }

    static String field(String[] tokens, String name) {
        for (String t : tokens) {
            if (t.startsWith(name + "=")) {
                return t;
            }
        }
        return name + "=?";
    }

    static String compression(Openssl openssl, int port) throws IOException, InterruptedException {
        String text = openssl.run("", "s_client", "-connect", "127.0.0.1:" + port, "-tls1_2", "-comp").output();
        Matcher m = Pattern.compile("Compression: (\\S+)").matcher(text);
        String result = m.find() ? m.group(1) : "unknown";
        return result.equals("NONE") ? "none: this OpenSSL build has no zlib" : "on, " + result;
    }

    static String renegotiation(Openssl openssl, int port, String... flags) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("s_client", "-connect", "127.0.0.1:" + port));
        args.addAll(List.of(flags.length == 0 ? new String[] {"-tls1_3"} : flags));
        String text = openssl.run("R\n", args.toArray(String[]::new)).output();
        if (text.contains("forbids renegotiation")) {
            return "refused by the client: TLS 1.3 forbids it";
        }
        if (text.contains("no renegotiation")) {
            return "refused by the server: no_renegotiation alert";
        }
        return text.contains("RENEGOTIATING") ? "went ahead" : "not tried";
    }
}
