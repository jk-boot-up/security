package com.jk.explore.security.tlsservers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

public class TlsServersDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static final String HOST = "www.shop.localhost";

    public static String run() throws IOException, InterruptedException, GeneralSecurityException {
        StringBuilder out = new StringBuilder();
        Path work = Files.createTempDirectory("tlsservers");
        LocalCa ca = new LocalCa(work.resolve("ca"));
        ca.issue("www", "24h", HOST);
        try (Nginx nginx = new Nginx(work.resolve("nginx"), ca.cert("www").toString(), ca.key("www").toString(), ca.root().toString());
                Caddy caddy = new Caddy(work.resolve("caddy"))) {
            act1(out, ca, nginx);
            act2(out, ca, nginx);
            act3(out, ca, nginx);
            act4(out, ca, caddy);
            act5(out, work, ca, nginx, caddy);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String outcome(Https.Response r) {
        return r.refused() ? "refused (" + r.refusal() + ")" : r.status() + " over " + r.protocol();
    }

    private static void act1(StringBuilder out, LocalCa ca, Nginx nginx) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "ONE. nginx with a certificate from the local authority.");
        nginx.start(Nginx.Settings.defaults());
        line(out, "  a client that trusts the lab's root asks for " + HOST + ": " + outcome(Https.get(nginx.httpsPort(), HOST, "/", Https.trusting(ca.root()))));
        line(out, "  a client that trusts only its own default roots: " + outcome(Https.get(nginx.httpsPort(), HOST, "/", javax.net.ssl.SSLContext.getDefault())));
        line(out, "  the same client, asking for pay.shop.localhost, which the certificate does not name: " + outcome(Https.get(nginx.httpsPort(), "pay.shop.localhost", "/", Https.trusting(ca.root()))));
        Https.Response redirect = Https.plain(nginx.httpPort(), HOST, "/cart");
        line(out, "  plain http on its own port: " + redirect.status() + ", sent on to https: " + redirect.header("location").startsWith("https://" + HOST));
        line(out, "  by default the Server header gives the exact version of nginx: " + Https.get(nginx.httpsPort(), HOST, "/", Https.trusting(ca.root())).header("server").matches("nginx/\\d.*"));
        nginx.restart(Nginx.Settings.defaults().serverTokens(false));
        line(out, "  with server_tokens off it says only: " + Https.get(nginx.httpsPort(), HOST, "/", Https.trusting(ca.root())).header("server"));
    }

    private static void act2(StringBuilder out, LocalCa ca, Nginx nginx) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "TWO. Which protocol versions it speaks.");
        var trust = Https.trusting(ca.root());
        nginx.restart(Nginx.Settings.defaults());
        line(out, "  the default accepts a client that offers only TLS 1.2: " + outcome(Https.get(nginx.httpsPort(), HOST, "/", trust, "TLSv1.2")) + "; and one that offers 1.3: " + outcome(Https.get(nginx.httpsPort(), HOST, "/", trust, "TLSv1.3")));
        nginx.restart(Nginx.Settings.defaults().protocols("TLSv1.3"));
        line(out, "  with ssl_protocols TLSv1.3, the client that offers only 1.2: " + outcome(Https.get(nginx.httpsPort(), HOST, "/", trust, "TLSv1.2")) + "; the one that offers 1.3: " + outcome(Https.get(nginx.httpsPort(), HOST, "/", trust, "TLSv1.3")));
        line(out, "  the price of the stricter setting is every client that cannot speak 1.3; the setting is one line, and a change needs a reload");
    }

    private static void act3(StringBuilder out, LocalCa ca, Nginx nginx) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "THREE. HSTS, and the add_header trap.");
        var trust = Https.trusting(ca.root());
        nginx.restart(Nginx.Settings.defaults());
        line(out, "  by default the response has Strict-Transport-Security: " + (Https.get(nginx.httpsPort(), HOST, "/", trust).header("strict-transport-security") != null));
        nginx.restart(Nginx.Settings.defaults().hsts(true));
        line(out, "  with add_header in the server block: " + Https.get(nginx.httpsPort(), HOST, "/", trust).header("strict-transport-security") + "; over plain http, where a browser would ignore it, it is sent: " + (Https.plain(nginx.httpPort(), HOST, "/").header("strict-transport-security") != null));
        nginx.restart(Nginx.Settings.defaults().hsts(true).accountHeader(true, false));
        line(out, "  /account has its own add_header (X-Frame-Options): the page has X-Frame-Options: " + Https.get(nginx.httpsPort(), HOST, "/account", trust).header("x-frame-options") + " and Strict-Transport-Security: " + (Https.get(nginx.httpsPort(), HOST, "/account", trust).header("strict-transport-security") != null) + "  <- A LOCATION'S add_header REPLACES THE SERVER'S");
        nginx.restart(Nginx.Settings.defaults().hsts(true).accountHeader(true, true));
        line(out, "  repeating it in the location: Strict-Transport-Security: " + (Https.get(nginx.httpsPort(), HOST, "/account", trust).header("strict-transport-security") != null));
        line(out, "  a browser that has seen the header refuses plain http to this name for a year; max-age is a promise that is hard to take back");
    }

    private static void act4(StringBuilder out, LocalCa ca, Caddy caddy) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "FOUR. Caddy makes its own certificates.");
        caddy.start(false);
        var trust = Https.trusting(caddy.root());
        Https.Response r = Https.get(caddy.httpsPort(), HOST, "/", trust);
        line(out, "  the Caddyfile names only the site; Caddy made a root, an intermediate and a certificate by itself: " + outcome(r) + ", chain of " + r.chain().size() + ", issued by " + r.leaf().getIssuerX500Principal().getName().replaceAll("^CN=([^,]*).*", "$1"));
        long hours = java.time.Duration.between(r.leaf().getNotBefore().toInstant(), r.leaf().getNotAfter().toInstant()).toHours();
        line(out, "  the certificate lasts " + hours + " hours, and Caddy renews it on its own");
        line(out, "  a client that trusts nginx's root but not Caddy's: " + outcome(Https.get(caddy.httpsPort(), HOST, "/", Https.trusting(ca.root()))));
        Https.Response redirect = Https.plain(caddy.httpPort(), HOST, "/cart");
        line(out, "  plain http: " + redirect.status() + ", sent on to https: " + redirect.header("location").startsWith("https://" + HOST));
        line(out, "  the Server header says only: " + r.header("server") + "; Strict-Transport-Security by default: " + (r.header("strict-transport-security") != null));
        line(out, "  a client that offers only TLS 1.2 is accepted by default: " + outcome(Https.get(caddy.httpsPort(), HOST, "/", trust, "TLSv1.2")));
        caddy.start(true);
        line(out, "  with a header line in the Caddyfile: " + Https.get(caddy.httpsPort(), HOST, "/", trust).header("strict-transport-security"));
    }

    private static void act5(StringBuilder out, Path work, LocalCa ca, Nginx nginx, Caddy caddy) throws IOException, InterruptedException, GeneralSecurityException {
        line(out, "FIVE. The bill.");
        var trust = Https.trusting(ca.root());
        nginx.restart(Nginx.Settings.defaults().stapling(true));
        Https.Response r = Https.get(nginx.httpsPort(), HOST, "/", trust);
        line(out, "  ssl_stapling on, and the client asks for a stapled OCSP answer: " + r.stapled() + " received; nginx says it found no responder to ask: " + nginx.errorLog().contains("no OCSP responder URL") + "  <- STAPLING NEEDS A RESPONDER IN THE CERTIFICATE");
        line(out, "  Caddy's local certificates carry no responder either, so nothing is stapled: " + Https.get(caddy.httpsPort(), HOST, "/", Https.trusting(caddy.root())).stapled() + " received");
        java.math.BigInteger before = r.leaf().getSerialNumber();
        ca.issue("www", "24h", HOST);
        java.math.BigInteger onDisk = Https.certificates(ca.cert("www")).get(0).getSerialNumber();
        java.math.BigInteger served = Https.get(nginx.httpsPort(), HOST, "/", trust).leaf().getSerialNumber();
        line(out, "  a new certificate is written over the old file: on disk it is new: " + !onDisk.equals(before) + ", nginx still serves the old one: " + served.equals(before) + "  <- NGINX READS THE FILE ONLY WHEN IT STARTS OR RELOADS");
        nginx.reload();
        line(out, "  after nginx -s reload it serves the new one: " + Https.get(nginx.httpsPort(), HOST, "/", trust).leaf().getSerialNumber().equals(onDisk));
        line(out, "  nginx needs a renewal job that ends with a reload; Caddy renews and swaps by itself, and needs its data folder kept, because that folder holds the keys of its local authority");
        line(out, "  every client needs the root of whichever authority signed: the lab's for nginx, Caddy's for Caddy; the browser's own list contains neither");
    }
}
