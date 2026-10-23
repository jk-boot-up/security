package com.jk.explore.security.workloadid;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import io.spiffe.bundle.x509bundle.X509Bundle;
import io.spiffe.exception.BundleNotFoundException;
import io.spiffe.provider.SpiffeSslContextFactory;
import io.spiffe.provider.SpiffeSslContextFactory.SslContextOptions;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.spiffeid.TrustDomain;
import io.spiffe.svid.x509svid.X509Svid;
import io.spiffe.workloadapi.DefaultX509Source;
import io.spiffe.workloadapi.DefaultX509Source.X509SourceOptions;
import io.spiffe.workloadapi.X509Source;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

/**
 * The program that runs inside the SPIRE container, once per workload. The role is the first argument.
 * It never says who it is: its identity is whatever the SPIRE agent hands to the operating system user it runs as.
 * Every role prints one line starting with RESULT, which is what the demo reads.
 */
public final class Workload {

    static final String SOCKET = "unix:/tmp/spire-agent/public/api.sock";
    static final Path WORK = Path.of("/work");

    private Workload() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("http.keepAlive", "false");
        switch (args[0]) {
            case "ledger" -> ledger(args[1]);
            case "whoami" -> whoami();
            case "call" -> call(args[1], args[2], args[3], args[4]);
            case "loop" -> loop(args[1], args[2], Integer.parseInt(args[3]));
            case "stolen" -> stolen(Path.of(args[1]), args[2], args[3], args[4], args[5]);
            case "forged" -> forged(args[1], args[2], args[3], args[4]);
            default -> throw new IllegalArgumentException(args[0]);
        }
    }

    // ---- the server (the ledger role: any workload with an identity can run it, the identity is what it is given)

    private static void ledger(String name) throws Exception {
        X509Source source = open(Duration.ofSeconds(15));
        SSLContext context = SpiffeSslContextFactory.getSslContext(SslContextOptions.builder().x509Source(source).acceptAnySpiffeId().build());
        HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters parameters = context.getDefaultSSLParameters();
                parameters.setNeedClientAuth(true);
                params.setSSLParameters(parameters);
            }
        });
        Path audit = WORK.resolve("audit-" + name + ".log");
        server.createContext("/", exchange -> {
            String caller = "none";
            try {
                Certificate[] chain = ((HttpsExchange) exchange).getSSLSession().getPeerCertificates();
                caller = spiffeIdOf((X509Certificate) chain[0]);
            } catch (Exception e) {
                caller = "unknown";
            }
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            int code = Policy.allows(caller, method, path) ? 200 : 403;
            Files.writeString(audit, caller + " " + method + " " + path + " " + code + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
        });
        server.start();
        Files.writeString(WORK.resolve("port-" + name), Integer.toString(server.getAddress().getPort()));
        Thread.currentThread().join();
    }

    /** The SPIFFE ID in the URI subject alternative name of a certificate. */
    static String spiffeIdOf(X509Certificate certificate) throws Exception {
        for (List<?> san : certificate.getSubjectAlternativeNames()) {
            if (((Integer) san.get(0)) == 6) {
                return (String) san.get(1);
            }
        }
        return "none";
    }

    // ---- the clients

    private static void whoami() {
        try (X509Source source = open(Duration.ofSeconds(Long.getLong("svid.wait", 20)))) {
            X509Certificate leaf = source.getX509Svid().getLeaf();
            long span = (leaf.getNotAfter().getTime() - leaf.getNotBefore().getTime()) / 1000;
            long remaining = (leaf.getNotAfter().getTime() - System.currentTimeMillis()) / 1000;
            System.out.println("RESULT id=" + source.getX509Svid().getSpiffeId() + " span=" + span + " remaining=" + remaining);
        } catch (Exception e) {
            System.out.println("RESULT no-svid");
        }
    }

    private static void call(String port, String expectedId, String method, String path) {
        try (X509Source source = open(Duration.ofSeconds(Long.getLong("svid.wait", 20)))) {
            System.out.println("RESULT " + request(context(source, expectedId), port, method, path).result());
        } catch (Exception e) {
            System.out.println("RESULT no-svid");
        }
    }

    /** Calls the ledger about four times a second on new connections and reports how often its own and the server's certificate changed. */
    private static void loop(String port, String expectedId, int seconds) throws Exception {
        List<BigInteger> own = new ArrayList<>();
        List<BigInteger> server = new ArrayList<>();
        int calls = 0;
        int failed = 0;
        try (X509Source source = open(Duration.ofSeconds(10))) {
            SSLContext context = context(source, expectedId);
            long end = System.currentTimeMillis() + seconds * 1000L;
            while (System.currentTimeMillis() < end) {
                own.add(source.getX509Svid().getLeaf().getSerialNumber());
                Outcome outcome = request(context, port, "POST", "/transfers");
                calls++;
                if (!outcome.result().equals("200")) {
                    failed++;
                }
                if (outcome.serverSerial() != null) {
                    server.add(outcome.serverSerial());
                }
                Thread.sleep(250);
            }
        }
        System.out.println("RESULT calls=" + calls + " failed=" + failed + " own=" + Rotations.count(own) + " server=" + Rotations.count(server));
    }

    /** An attacker with a copy of a workload's SVID files: certificate, private key and the public bundle. */
    private static void stolen(Path dir, String port, String expectedId, String method, String path) {
        try {
            X509Svid svid = X509Svid.load(dir.resolve("svid.0.pem"), dir.resolve("svid.0.key"));
            X509Bundle bundle = X509Bundle.load(svid.getSpiffeId().getTrustDomain(), dir.resolve("bundle.0.pem"));
            System.out.println("RESULT " + request(context(new Copy(svid, bundle), expectedId), port, method, path).result());
        } catch (Exception e) {
            System.out.println("RESULT error " + e.getClass().getSimpleName());
        }
    }

    /** A certificate that names itself spiffe://bank.example/payments but was made by the attacker, not by SPIRE. */
    private static void forged(String keystore, String bundlePem, String port, String path) {
        try {
            KeyStore mine = KeyStore.getInstance("PKCS12");
            try (FileInputStream in = new FileInputStream(keystore)) {
                mine.load(in, "lab-password".toCharArray());
            }
            KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(mine, "lab-password".toCharArray());
            KeyStore trusted = KeyStore.getInstance("PKCS12");
            trusted.load(null, null);
            int n = 0;
            for (X509Certificate ca : X509Bundle.load(TrustDomain.parse("bank.example"), Path.of(bundlePem)).getX509Authorities()) {
                trusted.setCertificateEntry("spire-" + n++, ca);
            }
            TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trust.init(trusted);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keys.getKeyManagers(), trust.getTrustManagers(), null);
            System.out.println("RESULT " + request(context, port, "POST", path).result());
        } catch (Exception e) {
            System.out.println("RESULT error " + e.getClass().getSimpleName());
        }
    }

    // ---- shared pieces

    private static X509Source open(Duration wait) throws Exception {
        return DefaultX509Source.newSource(X509SourceOptions.builder().spiffeSocketPath(SOCKET).initTimeout(wait).build());
    }

    /** A TLS context whose certificate comes from the source (read again at every handshake) and which accepts only the one server identity. */
    private static SSLContext context(X509Source source, String expectedId) throws Exception {
        return SpiffeSslContextFactory.getSslContext(SslContextOptions.builder().x509Source(source)
                .acceptedSpiffeIdsSupplier(() -> Set.of(SpiffeId.parse(expectedId))).build());
    }

    /** What a call came to: the HTTP status, or refused when the connection failed. */
    record Outcome(String result, BigInteger serverSerial) {
    }

    private static Outcome request(SSLContext context, String port, String method, String path) {
        try {
            String number = Files.readString(WORK.resolve("port-" + port), StandardCharsets.UTF_8).trim();
            HttpsURLConnection connection = (HttpsURLConnection) URI.create("https://localhost:" + number + path).toURL().openConnection();
            connection.setSSLSocketFactory(context.getSocketFactory());
            // the identity is the SPIFFE ID checked by the context, not the host name
            connection.setHostnameVerifier((host, session) -> true);
            connection.setRequestMethod(method);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            if (method.equals("POST")) {
                connection.setDoOutput(true);
                connection.getOutputStream().write("{}".getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            BigInteger serial = ((X509Certificate) connection.getServerCertificates()[0]).getSerialNumber();
            connection.disconnect();
            return new Outcome(Integer.toString(code), serial);
        } catch (IOException e) {
            return new Outcome("refused", null);
        }
    }

    /** An SVID and a bundle read from files, the way a copy of them would be used. */
    private record Copy(X509Svid svid, X509Bundle bundle) implements X509Source {

        @Override
        public X509Svid getX509Svid() {
            return svid;
        }

        @Override
        public X509Bundle getBundleForTrustDomain(TrustDomain trustDomain) throws BundleNotFoundException {
            if (!trustDomain.equals(bundle.getTrustDomain())) {
                throw new BundleNotFoundException("no bundle for " + trustDomain);
            }
            return bundle;
        }

        @Override
        public void close() {
        }
    }
}
