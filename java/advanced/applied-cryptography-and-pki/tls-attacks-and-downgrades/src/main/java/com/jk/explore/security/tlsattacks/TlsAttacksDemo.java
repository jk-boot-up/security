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
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
        return set.isEmpty() ? "none" : String.join(", ", set.stream().map(Weakness::label).toList());
    }

    static Set<Weakness> missed(Set<Weakness> found) {
        Set<Weakness> m = EnumSet.copyOf(Weakness.PLANTED);
        m.removeAll(found);
        return m;
    }

    public static String run(Path buildDir) throws Exception {
        StringBuilder out = new StringBuilder();
        Openssl openssl = Openssl.find();
        Path testssl = TestSsl.install(buildDir);
        try (Lab lab = new Lab(openssl);
                Server weak = new Server(lab, Server.Profile.WEAK);
                Server strong = new Server(lab, Server.Profile.STRONG);
                Server only13 = new Server(lab, Server.Profile.TLS13_ONLY)) {
            int w = weak.port();
            int s = strong.port();
            int t = only13.port();

            out.append("--- the lab\n");
            out.append("three local servers (openssl s_server on 127.0.0.1): weak, strong, tls 1.3 only\n");
            out.append("testssl.sh pinned to " + TestSsl.TAG + ": yes\n");
            out.append("weaknesses planted in the weak server: " + Weakness.PLANTED.size() + "\n");

            CompletableFuture<List<TestSsl.Finding>> weakScan = scanAsync(testssl, openssl, lab, "weak.json", w);
            CompletableFuture<List<TestSsl.Finding>> strongScan = scanAsync(testssl, openssl, lab, "strong.json", s);

            out.append("--- what each server accepts, found by connecting with openssl s_client\n");
            for (int i = 0; i < Audit.VERSIONS.length; i++) {
                out.append(row("protocol " + Audit.VERSION_NAMES[i], Audit.version(openssl, w, i).connected(),
                        Audit.version(openssl, s, i).connected(), Audit.version(openssl, t, i).connected()));
            }
            for (int i = 0; i < Audit.SUITES.length; i++) {
                out.append(row("suite " + Audit.SUITES[i][0] + " (" + Audit.SUITES[i][1] + ")", Audit.suite(openssl, w, i).connected(),
                        Audit.suite(openssl, s, i).connected(), Audit.suite(openssl, t, i).connected()));
            }
            out.append("DH group size the weak server sends with DHE-RSA-AES128-GCM-SHA256: " + Audit.suite(openssl, w, 2).dhBits() + " bits\n");

            out.append("--- our own probes scored against the planted weaknesses\n");
            Set<Weakness> probesWeak = Audit.find(openssl, w);
            Set<Weakness> probesStrong = Audit.find(openssl, s);
            out.append("weak server: found " + probesWeak.size() + " of " + Weakness.PLANTED.size() + ", missed: " + names(missed(probesWeak)) + "\n");
            out.append("strong server: planted weaknesses found " + probesStrong.size() + ", false alarms: " + names(probesStrong) + "\n");

            out.append("--- testssl.sh " + TestSsl.TAG + " against the same servers\n");
            List<TestSsl.Finding> weakReport = weakScan.get();
            List<TestSsl.Finding> strongReport = strongScan.get();
            Set<Weakness> tsWeak = TestSsl.detect(weakReport);
            Set<Weakness> tsStrong = TestSsl.detect(strongReport);
            out.append("weak server: found " + tsWeak.size() + " of " + Weakness.PLANTED.size() + ", missed: " + names(missed(tsWeak)) + "\n");
            Set<Weakness> byTable = EnumSet.of(Weakness.STATIC_RSA, Weakness.SMALL_DH);
            byTable.retainAll(tsWeak);
            out.append("found by a finding that names the problem: " + (tsWeak.size() - byTable.size()) + ", found only by reading the key exchange column of its cipher table: "
                    + byTable.size() + " (" + names(byTable) + ")\n");
            out.append("suites with static RSA key exchange in its cipher table: " + new TreeSet<>(TestSsl.kx(weakReport, "RSA")).size() + "\n");
            out.append("strong server: planted weaknesses found " + tsStrong.size() + ", findings of LOW or worse: " + TestSsl.atLeastLow(strongReport) + "\n");

            downgrade(out, openssl, w);
            forwardSecrecy(out, openssl, lab, w);
            tls13(out, openssl, lab, w, t);
        }
        out.append("--- the bill\n");
        out.append("a scanner reports what a server offers; it does not say who can still read yesterday's traffic\n");
        out.append("a downgrade needs a network that fails and a client that retries: remove the retry or sign it with the SCSV\n");
        out.append("forward secrecy is a property of the key exchange, not of the key length\n");
        out.append("a clean scan today says nothing about traffic recorded under the old settings\n");
        return out.toString();
    }

    static String row(String what, boolean weak, boolean strong, boolean only13) {
        return what + ": weak " + yn(weak) + ", strong " + yn(strong) + ", tls 1.3 only " + yn(only13) + "\n";
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

    // ------------------------------------------------------------------ downgrade

    /** One connection attempt of the legacy client: the flags it uses for the version it tries. */
    record Attempt(String label, String[] flags) {
    }

    /** What a client that retries ended with. */
    record Outcome(boolean connected, int attempts, String protocol, String cipher, String alert) {
    }

    /** A client that retries with lower versions when a handshake fails, with or without the fallback signal, never below a minimum. */
    static Outcome dance(Openssl openssl, int port, boolean scsv, int minVersionIndex) throws IOException, InterruptedException {
        String[] versionFlags = {"-tls1", "-tls1_1", "-tls1_2", ""};
        int attempts = 0;
        String lastAlert = "";
        for (int i = 3; i >= minVersionIndex; i--) {
            List<String> flags = new ArrayList<>();
            if (!versionFlags[i].isEmpty()) {
                flags.add(versionFlags[i]);
            }
            flags.addAll(List.of("-cipher", "DEFAULT:@SECLEVEL=0"));
            if (scsv && attempts > 0) {
                flags.add("-fallback_scsv");
            }
            Probe p = Probe.run(openssl, port, "", flags.toArray(String[]::new));
            attempts++;
            if (p.connected()) {
                return new Outcome(true, attempts, p.protocol(), p.cipher(), "");
            }
            lastAlert = p.alert();
        }
        return new Outcome(false, attempts, "", "", lastAlert.isEmpty() ? "none, the connection was reset" : lastAlert);
    }

    static void downgrade(StringBuilder out, Openssl openssl, int weakPort) throws Exception {
        out.append("--- the downgrade dance\n");
        try (Tap clean = new Tap(weakPort, 0xFFFF)) {
            Probe.run(openssl, clean.port(), "", "-tls1_2", "-cipher", "DEFAULT:@SECLEVEL=0");
            Probe.run(openssl, clean.port(), "");
            List<Tap.Capture> c = clean.await(2);
            out.append("server that could speak TLS 1.3, asked for TLS 1.2: ServerHello random says " + sentinel(c, 0) + "\n");
            out.append("same server, asked for TLS 1.3: ServerHello random says " + sentinel(c, 1) + "\n");
        }
        try (Tap breaker = new Tap(weakPort, 0x0301)) {
            out.append("the network breaks every handshake that offers more than TLS 1.0\n");
            Outcome plain = dance(openssl, breaker.port(), false, 0);
            out.append("client that falls back with no signal: connected " + yn(plain.connected()) + ", attempts " + plain.attempts() + ", ended on " + plain.protocol() + " with " + plain.cipher() + "\n");
            Outcome scsv = dance(openssl, breaker.port(), true, 0);
            out.append("client that falls back and sends TLS_FALLBACK_SCSV: connected " + yn(scsv.connected()) + ", attempts " + scsv.attempts() + ", server answer: " + scsv.alert() + "\n");
            Outcome floor = dance(openssl, breaker.port(), false, 2);
            out.append("client with a minimum of TLS 1.2: connected " + yn(floor.connected()) + ", attempts " + floor.attempts() + ", it never offered TLS 1.0\n");
            out.append("JDK client, default settings, protocols enabled: " + String.join(", ", SSLContext.getDefault().getDefaultSSLParameters().getProtocols()) + "\n");
            out.append("JDK client through the same network: handshake failed " + yn(!jdkConnects(breaker.port())) + ", retried with an older version: no\n");
        }
    }

    static String sentinel(List<Tap.Capture> captures, int i) {
        byte[] hello = TlsRecords.message(TlsRecords.clearHandshake(TlsRecords.records(captures.get(i).serverToClient())), TlsRecords.SERVER_HELLO);
        return hello == null ? "nothing" : TlsRecords.downgradeSentinel(hello).equals("none") ? "no downgrade marker" : "DOWNGRD, a downgrade to " + TlsRecords.downgradeSentinel(hello);
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

    // ------------------------------------------------------------------ forward secrecy

    static void forwardSecrecy(StringBuilder out, Openssl openssl, Lab lab, int weakPort) throws Exception {
        out.append("--- a recorded session, and the server key leaks later\n");
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair unrelated = g.generateKeyPair();
        String[][] suites = {{"AES128-GCM-SHA256", "static RSA"}, {"DHE-RSA-AES128-GCM-SHA256", "finite-field DH"}, {"ECDHE-RSA-AES128-GCM-SHA256", "elliptic curve DH"}};
        for (String[] suite : suites) {
            try (Tap tap = new Tap(weakPort, 0xFFFF)) {
                Probe p = Probe.run(openssl, tap.port(), SECRET_REQUEST + "\n", "-tls1_2", "-no_ems", "-cipher", suite[0] + ":@SECLEVEL=0");
                List<Tap.Capture> c = tap.await(1);
                Tap.Capture cap = c.get(0);
                Optional<String> leaked = Decryptor.clientData(cap.clientToServer(), cap.serverToClient(), lab.keyPair().getPrivate());
                Optional<String> other = Decryptor.clientData(cap.clientToServer(), cap.serverToClient(), unrelated.getPrivate());
                out.append(suite[0] + " (" + suite[1] + "): negotiated " + yn(p.connected() && suite[0].equals(p.cipher())) + ", request visible in the recording: "
                        + yn(contains(cap.clientToServer(), SECRET_REQUEST)) + "\n");
                out.append("  opened with the leaked server key: " + leaked.map(text -> "yes, it says " + text.lines().findFirst().orElse("")).orElse("no")
                        + "; opened with an unrelated key: " + yn(other.isPresent()) + "\n");
            }
        }
        out.append("why: in static RSA the client sends the secret encrypted to the server key; in (EC)DHE the secret is agreed from keys thrown away after the handshake\n");
    }

    static boolean contains(byte[] haystack, String needle) {
        return new String(haystack, StandardCharsets.ISO_8859_1).contains(needle);
    }

    // ------------------------------------------------------------------ TLS 1.3

    static void tls13(StringBuilder out, Openssl openssl, Lab lab, int weakPort, int only13Port) throws Exception {
        out.append("--- what TLS 1.3 removed, checked against a TLS 1.3 only server\n");
        List<String> suites = List.of(openssl.run("", "ciphers", "-v", "-tls1_3").output().split("\n")).stream().filter(l -> l.contains("TLSv1.3")).toList();
        long aead = suites.stream().filter(l -> l.contains("Mac=AEAD")).count();
        long anyKx = suites.stream().filter(l -> l.contains("Kx=any")).count();
        int refused = 0;
        String alert = "";
        for (String v : new String[] {"-tls1", "-tls1_1", "-tls1_2"}) {
            Probe p = Probe.with(openssl, only13Port, v, "DEFAULT:AES128-GCM-SHA256");
            if (!p.connected()) {
                refused++;
                alert = p.alert();
            }
        }
        out.append("older versions (TLS 1.0, 1.1, 1.2 as a fallback): refused " + refused + " of 3, server says: " + alert + "\n");
        Probe rsa = Probe.with(openssl, only13Port, "-tls1_2", "AES128-GCM-SHA256");
        out.append("static RSA key exchange: client asks for AES128-GCM-SHA256, connected " + yn(rsa.connected()) + "; TLS 1.3 suites with a fixed key exchange: " + (suites.size() - anyKx) + " of " + suites.size() + "\n");
        Probe cbc = Probe.with(openssl, only13Port, "-tls1_2", "AES128-SHA");
        out.append("CBC with a MAC: client asks for AES128-SHA, connected " + yn(cbc.connected()) + "; TLS 1.3 suites that are not AEAD: " + (suites.size() - aead) + " of " + suites.size() + "\n");
        boolean rc4 = openssl.run("", "ciphers", "RC4").output().contains("no cipher match");
        out.append("RC4: this OpenSSL build has RC4 suites: " + yn(!rc4) + (rc4 ? " (so a server that offers it cannot be built here; the AEAD only list above is what TLS 1.3 allows)" : "") + "\n");
        try (Tap tap = new Tap(only13Port, 0xFFFF)) {
            Probe.run(openssl, tap.port(), "", "-tls1_3", "-comp");
            byte[] hello = TlsRecords.message(TlsRecords.clearHandshake(TlsRecords.records(tap.await(1).get(0).clientToServer())), TlsRecords.CLIENT_HELLO);
            out.append("compression: a TLS 1.3 ClientHello started with -comp offers methods " + TlsRecords.compressionMethods(hello) + " (0 is null, no compression)\n");
        }
        out.append("compression in TLS 1.2: negotiated with -comp on both ends of the weak server: " + compression(openssl, weakPort) + "\n");
        out.append("renegotiation: client asks TLS 1.3 to renegotiate: " + renegotiation(openssl, only13Port) + "\n");
        out.append("renegotiation: client asks TLS 1.2 on the weak server to renegotiate: " + renegotiation(openssl, weakPort, "-tls1_2") + "\n");
        Probe dh = Probe.run(openssl, only13Port, "", "-tls1_3");
        out.append("custom DH groups: the server's file has a 1024-bit group; TLS 1.3 negotiated a named group: " + yn(!dh.group().isEmpty()) + ", used the 1024-bit group: " + yn(dh.dhBits() == 1024) + "\n");
    }

    static String compression(Openssl openssl, int port) throws IOException, InterruptedException {
        String text = openssl.run("", "s_client", "-connect", "127.0.0.1:" + port, "-tls1_2", "-comp").output();
        Matcher m = Pattern.compile("Compression: (\\S+)").matcher(text);
        String result = m.find() ? m.group(1) : "unknown";
        return result.equals("NONE") ? "no (this OpenSSL build has no zlib, so CRIME could not be shown)" : "yes, " + result;
    }

    static String renegotiation(Openssl openssl, int port, String... flags) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("s_client", "-connect", "127.0.0.1:" + port));
        args.addAll(List.of(flags.length == 0 ? new String[] {"-tls1_3"} : flags));
        String text = openssl.run("R\n", args.toArray(String[]::new)).output();
        if (text.contains("forbids renegotiation")) {
            return "refused by the client library, \"this TLS version forbids renegotiation\"";
        }
        if (text.contains("no renegotiation")) {
            return "refused by the server, a no_renegotiation alert";
        }
        return text.contains("RENEGOTIATING") ? "went ahead" : "not tried";
    }
}
