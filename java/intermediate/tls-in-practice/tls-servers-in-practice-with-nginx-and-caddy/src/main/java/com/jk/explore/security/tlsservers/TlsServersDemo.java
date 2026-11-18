package com.jk.explore.security.tlsservers;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;

/**
 * Starts a real nginx and a real Caddy on loopback ports in front of the same shop and prints what a client sees.
 * Ports, the temporary folder, serial numbers, dates and the nginx version are printed as placeholders, so the output never changes.
 */
public class TlsServersDemo {

    static final String HOST = "www.shop.localhost";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** What the demo prints, with the values that change from run to run masked by {@link #mask}. */
    private record Masks(List<String> folders, int... ports) {
    }

    public static String run() throws IOException, InterruptedException, GeneralSecurityException {
        StringBuilder out = new StringBuilder();
        Path work = Files.createTempDirectory("tlsservers");
        try {
            LocalCa ca = new LocalCa(work.resolve("ca"));
            ca.issue("www", "24h", HOST);
            try (Nginx nginx = new Nginx(work.resolve("nginx"), ca.cert("www").toString(), ca.key("www").toString(), ca.root().toString());
                    Caddy caddy = new Caddy(work.resolve("caddy"))) {
                Masks m = new Masks(List.of(work.toRealPath().toString(), work.toString()), nginx.httpPort(), nginx.httpsPort(), caddy.httpPort(), caddy.httpsPort());
                act1(out, m, ca, nginx);
                act2(out, m, ca, nginx);
                act3(out, m, ca, nginx);
                act4(out, m, ca, caddy);
                act5(out, m, ca, nginx, caddy);
            }
        } finally {
            delete(work);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** Replaces the temporary folder and the ports with placeholders; the ports always follow a colon or "_port ". */
    private static String mask(Masks m, String text) {
        String s = text;
        for (String folder : m.folders()) {
            s = s.replace(folder, "<work>");
        }
        String[] names = {"<http-port>", "<https-port>", "<http-port>", "<https-port>"};
        for (int i = 0; i < m.ports().length; i++) {
            s = s.replaceAll("(?<=[:]|_port )" + m.ports()[i] + "\\b", names[i]);
        }
        return s;
    }

    /** The lines of a configuration that start with one of {@code starts}, stripped, masked and wrapped before a long quoted value. */
    private static List<String> lines(Masks m, String config, String... starts) {
        List<String> shown = new ArrayList<>();
        for (String raw : config.split("\n")) {
            String l = raw.strip();
            if (Arrays.stream(starts).anyMatch(l::startsWith)) {
                shown.addAll(wrap(mask(m, l)));
            }
        }
        return shown;
    }

    private static List<String> wrap(String l) {
        int quote = l.indexOf(" \"");
        return l.length() > 60 && quote > 0 ? List.of(l.substring(0, quote), "  " + l.substring(quote + 1)) : List.of(l);
    }

    private static String outcome(Https.Response r) {
        return r.refused() ? "refused: " + r.refusal() : r.status() + " over " + r.protocol();
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    private static String orNone(String header) {
        return header == null ? "(none)" : header;
    }

    private static void act1(StringBuilder out, Masks m, LocalCa ca, Nginx nginx) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "ONE. nginx with a certificate from the local authority.");
        X509Certificate cert = Https.certificates(ca.cert("www")).get(0);
        line(out, "THE CERTIFICATE (made with step, signed by the lab's root):");
        line(out, "  subject   " + ChainCheck.commonName(cert.getSubjectX500Principal().getName()));
        line(out, "  names     " + ChainCheck.names(cert));
        line(out, "  issuer    " + ChainCheck.commonName(cert.getIssuerX500Principal().getName()));
        line(out, "  lifetime  " + Duration.between(cert.getNotBefore().toInstant(), cert.getNotAfter().toInstant()).toHours() + " hours");
        nginx.start(Nginx.Settings.defaults());
        line(out, "THE NGINX LINES (the https server block):");
        for (String l : lines(m, nginx.config(Nginx.Settings.defaults()), "listen 127.0.0.1:" + nginx.httpsPort(), "ssl_certificate")) {
            line(out, "  " + l);
        }
        SSLContext lab = Https.trusting(ca.root());
        String[] trusts = {"the lab's root", "default roots ", "the lab's root"};
        String[] names = {HOST, HOST, "pay.shop.localhost"};
        SSLContext[] contexts = {lab, SSLContext.getDefault(), lab};
        List<List<X509Certificate>> roots = List.of(Https.certificates(ca.root()), ChainCheck.defaultRoots(), Https.certificates(ca.root()));
        Https.Response[] responses = new Https.Response[3];
        for (int i = 0; i < 3; i++) {
            responses[i] = Https.get(nginx.httpsPort(), names[i], "/", contexts[i]);
        }
        List<X509Certificate> served = responses[0].chain();
        line(out, "THE REQUESTS (the roots the client trusts, the name it asks):");
        for (int i = 0; i < 3; i++) {
            line(out, "  " + (i + 1) + "  " + trusts[i] + "    " + names[i]);
        }
        line(out, "THE CHECKS, BY HAND (signed by a trusted root? name listed?):");
        for (int i = 0; i < 3; i++) {
            ChainCheck.Verdict v = ChainCheck.check(served, roots.get(i), names[i]);
            line(out, "  " + (i + 1) + "  " + v.signer() + " trusted: " + yes(v.trusted()) + (v.trusted() ? "; " + v.name() + " listed: " + yes(v.listed()) : ""));
        }
        line(out, "THE RESPONSES:");
        for (int i = 0; i < 3; i++) {
            line(out, "  " + (i + 1) + "  " + outcome(responses[i]));
        }
        Https.Response redirect = Https.plain(nginx.httpPort(), HOST, "/cart");
        line(out, "THE REQUEST (plain http):");
        line(out, "  " + mask(m, "GET http://" + HOST + ":" + nginx.httpPort() + "/cart"));
        line(out, "THE RESPONSE:");
        line(out, "  " + redirect.status() + "  Location: " + mask(m, redirect.header("location")));
        line(out, "BEFORE THE FIX (server_tokens on, the default):");
        line(out, "  Server: " + Https.get(nginx.httpsPort(), HOST, "/", lab).header("server").replaceAll("\\d+(\\.\\d+)*", "<version>"));
        Nginx.Settings hidden = Nginx.Settings.defaults().serverTokens(false);
        nginx.restart(hidden);
        line(out, "AFTER THE FIX (" + lines(m, nginx.config(hidden), "server_tokens").get(0) + "):");
        line(out, "  Server: " + Https.get(nginx.httpsPort(), HOST, "/", lab).header("server"));
    }

    private static void act2(StringBuilder out, Masks m, LocalCa ca, Nginx nginx) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "TWO. Which protocol versions it speaks.");
        SSLContext trust = Https.trusting(ca.root());
        String[] clients = {"old client", "new client"};
        String[] offers = {"TLSv1.2", "TLSv1.3"};
        line(out, "THE CLIENTS (what each one offers):");
        for (int i = 0; i < 2; i++) {
            line(out, "  " + clients[i] + "  [" + offers[i] + "]");
        }
        for (Nginx.Settings s : List.of(Nginx.Settings.defaults(), Nginx.Settings.defaults().protocols("TLSv1.3"))) {
            boolean before = s.equals(Nginx.Settings.defaults());
            nginx.restart(s);
            line(out, before ? "BEFORE THE CHANGE (the default line):" : "AFTER THE CHANGE (one line):");
            line(out, "  " + lines(m, nginx.config(s), "ssl_protocols").get(0));
            List<String> server = List.of(s.protocols().split(" "));
            line(out, "THE AGREEMENT, BY HAND (the highest version both speak):");
            for (int i = 0; i < 2; i++) {
                line(out, "  " + clients[i] + "  " + server + " and [" + offers[i] + "] -> " + Negotiation.agree(server, List.of(offers[i])).orElse("none"));
            }
            line(out, "THE RESPONSES:");
            for (int i = 0; i < 2; i++) {
                line(out, "  " + clients[i] + "  " + outcome(Https.get(nginx.httpsPort(), HOST, "/", trust, offers[i])));
            }
        }
    }

    private static void act3(StringBuilder out, Masks m, LocalCa ca, Nginx nginx) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "THREE. HSTS, and the add_header trap.");
        SSLContext trust = Https.trusting(ca.root());
        nginx.restart(Nginx.Settings.defaults());
        line(out, "BEFORE THE FIX (no add_header line):");
        line(out, "  GET / over https  Strict-Transport-Security: " + orNone(Https.get(nginx.httpsPort(), HOST, "/", trust).header("strict-transport-security")));
        Nginx.Settings hsts = Nginx.Settings.defaults().hsts(true);
        nginx.restart(hsts);
        line(out, "THE LINE ADDED TO THE SERVER BLOCK:");
        for (String l : lines(m, nginx.config(hsts), "add_header")) {
            line(out, "  " + l);
        }
        String value = Https.get(nginx.httpsPort(), HOST, "/", trust).header("strict-transport-security");
        line(out, "AFTER THE FIX (Strict-Transport-Security in the response):");
        line(out, "  GET / over https  " + orNone(value));
        line(out, "  GET / over http   " + orNone(Https.plain(nginx.httpPort(), HOST, "/").header("strict-transport-security")));
        long seconds = Headers.maxAge(value);
        line(out, "THE max-age, WORKED OUT:");
        line(out, "  " + seconds + " s / " + Headers.SECONDS_A_DAY + " s a day = " + seconds / Headers.SECONDS_A_DAY + " days");
        line(out, "  includeSubDomains: " + yes(Headers.includesSubDomains(value)) + ", so every name under " + HOST + " too");

        Nginx.Settings trap = hsts.accountHeader(true, false);
        nginx.restart(trap);
        line(out, "THE TRAP (/account has an add_header of its own):");
        boolean inAccount = false;
        for (String raw : nginx.config(trap).split("\n")) {
            String l = raw.strip();
            inAccount |= l.startsWith("location /account");
            if (inAccount && !l.isEmpty() && !l.startsWith("return")) {
                line(out, "  " + (l.startsWith("location") || l.equals("}") ? l : "    " + l));
            }
            inAccount &= !l.equals("}");
        }
        String config = nginx.config(trap);
        int at = config.indexOf("location /account");
        List<String> server = addHeaders(config.substring(config.indexOf(" ssl;"), config.indexOf("location /")));
        List<String> location = addHeaders(config.substring(at, config.indexOf('}', at)));
        line(out, "THE RULE, BY HAND (a location's own lines replace the server's):");
        line(out, "  server    " + server);
        line(out, "  location  " + location);
        line(out, "  applied   " + Headers.effective(server, location));
        Https.Response account = Https.get(nginx.httpsPort(), HOST, "/account", trust);
        line(out, "THE RESPONSE FROM GET /account:");
        line(out, "  X-Frame-Options: " + orNone(account.header("x-frame-options")));
        line(out, "  Strict-Transport-Security: " + orNone(account.header("strict-transport-security")) + "  <- THE SERVER'S LINE IS LOST");
        nginx.restart(hsts.accountHeader(true, true));
        account = Https.get(nginx.httpsPort(), HOST, "/account", trust);
        line(out, "AFTER THE FIX (the line repeated in the location):");
        line(out, "  X-Frame-Options: " + orNone(account.header("x-frame-options")));
        line(out, "  Strict-Transport-Security: " + orNone(account.header("strict-transport-security")));
    }

    private static void act4(StringBuilder out, Masks m, LocalCa ca, Caddy caddy) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "FOUR. Caddy makes its own certificates.");
        caddy.start(false);
        line(out, "THE CADDYFILE (the site block, all of it):");
        boolean inSite = false;
        for (String raw : caddy.caddyfile(false).split("\n")) {
            inSite |= raw.startsWith(HOST);
            if (inSite && !raw.isBlank()) {
                line(out, "  " + raw.stripTrailing());
            }
        }
        SSLContext trust = Https.trusting(caddy.root());
        Https.Response r = Https.get(caddy.httpsPort(), HOST, "/", trust);
        X509Certificate root = Https.certificates(caddy.root()).get(0);
        line(out, "THE CHAIN CADDY MADE BY ITSELF:");
        line(out, "  " + String.join(", ", ChainCheck.names(r.leaf())) + "  (sent by the server)");
        line(out, "    signed by " + ChainCheck.commonName(r.chain().get(1).getSubjectX500Principal().getName()) + "  (sent)");
        line(out, "    signed by " + ChainCheck.commonName(root.getSubjectX500Principal().getName()).replaceAll("\\d{4}", "<year>") + "  (root)");
        long seconds = Duration.between(r.leaf().getNotBefore().toInstant(), r.leaf().getNotAfter().toInstant()).toSeconds();
        line(out, "THE LIFETIME, WORKED OUT (not after - not before):");
        line(out, "  " + seconds + " s / 3600 s an hour = " + seconds / 3600 + " hours");
        line(out, "THE REQUESTS AND RESPONSES:");
        line(out, "  trusts Caddy's root    " + outcome(r));
        line(out, "  trusts the lab's root  " + outcome(Https.get(caddy.httpsPort(), HOST, "/", Https.trusting(ca.root()))));
        line(out, "  offers only TLS 1.2    " + outcome(Https.get(caddy.httpsPort(), HOST, "/", trust, "TLSv1.2")));
        Https.Response redirect = Https.plain(caddy.httpPort(), HOST, "/cart");
        line(out, "  plain http GET /cart   " + redirect.status());
        line(out, "    Location: " + mask(m, redirect.header("location")));
        line(out, "BEFORE THE FIX (the headers by default):");
        line(out, "  Server: " + orNone(r.header("server")));
        line(out, "  Strict-Transport-Security: " + orNone(r.header("strict-transport-security")));
        caddy.start(true);
        line(out, "AFTER THE FIX (one line in the site block):");
        for (String l : lines(m, caddy.caddyfile(true), "header")) {
            line(out, "  " + l);
        }
        line(out, "  Strict-Transport-Security: " + orNone(Https.get(caddy.httpsPort(), HOST, "/", Https.trusting(caddy.root())).header("strict-transport-security")));
    }

    private static void act5(StringBuilder out, Masks m, LocalCa ca, Nginx nginx, Caddy caddy) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "FIVE. The bill.");
        SSLContext trust = Https.trusting(ca.root());
        Nginx.Settings stapling = Nginx.Settings.defaults().stapling(true);
        nginx.restart(stapling);
        line(out, "THE LINES (stapling: the server attaches a revocation answer):");
        for (String l : lines(m, nginx.config(stapling), "ssl_stapling")) {
            for (String part : l.split("(?<=;) ")) {
                line(out, "  " + part);
            }
        }
        Https.Response r = Https.get(nginx.httpsPort(), HOST, "/", trust);
        Https.Response c = Https.get(caddy.httpsPort(), HOST, "/", Https.trusting(caddy.root()));
        line(out, "THE CERTIFICATES (does one name an OCSP responder?):");
        line(out, "  nginx's  " + yes(ChainCheck.namesAResponder(r.leaf())));
        line(out, "  Caddy's  " + yes(ChainCheck.namesAResponder(c.leaf())));
        line(out, "THE RESULT (stapled OCSP answers the client received):");
        line(out, "  nginx  " + r.stapled() + "  <- STAPLING NEEDS A RESPONDER IN THE CERTIFICATE");
        line(out, "  Caddy  " + c.stapled());
        String warning = nginx.errorLog().lines().filter(l -> l.contains("OCSP responder")).findFirst().orElse("(none)");
        line(out, "NGINX'S ERROR LOG:");
        line(out, "  " + mask(m, warning.replaceAll("^.*?#\\d+: ", "").replaceAll(" in the certificate.*", " in the certificate")));

        BigInteger old = r.leaf().getSerialNumber();
        ca.issue("www", "24h", HOST);
        BigInteger fresh = Https.certificates(ca.cert("www")).get(0).getSerialNumber();
        line(out, "THE RENEWAL (a new certificate written over the old file):");
        line(out, "  on disk          " + serial(fresh, old, fresh));
        line(out, "  served by nginx  " + serial(Https.get(nginx.httpsPort(), HOST, "/", trust).leaf().getSerialNumber(), old, fresh)
                + "  <- READ ONLY AT START OR RELOAD");
        nginx.reload();
        line(out, "AFTER nginx -s reload:");
        line(out, "  served by nginx  " + serial(Https.get(nginx.httpsPort(), HOST, "/", trust).leaf().getSerialNumber(), old, fresh));
    }

    /** The header names of the add_header lines in a piece of nginx configuration. */
    private static List<String> addHeaders(String block) {
        return java.util.regex.Pattern.compile("add_header (\\S+)").matcher(block).results().map(r -> r.group(1)).toList();
    }

    /** A serial number as a placeholder: which of the two certificates it belongs to. */
    private static String serial(BigInteger serial, BigInteger old, BigInteger fresh) {
        return serial.equals(old) ? "<old serial>" : serial.equals(fresh) ? "<new serial>" : "<another serial>";
    }

    private static void delete(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
