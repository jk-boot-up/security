package com.jk.explore.security.httpsspring;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ConfigurableApplicationContext;

/** A running store: a Spring Boot server on two free loopback ports (HTTPS and plain HTTP), configured by properties that name SSL bundles. */
final class Shop implements AutoCloseable {

    final ConfigurableApplicationContext context;
    final int httpsPort;
    final int httpPort;
    final Path folder;

    private Shop(ConfigurableApplicationContext context, int httpsPort, int httpPort, Path folder) {
        this.context = context;
        this.httpsPort = httpsPort;
        this.httpPort = httpPort;
        this.folder = folder;
    }

    /** What the store is told through properties, which is where TLS is configured. */
    record Settings(String serverKeystore, boolean requireClientCertificate, boolean reload) {

        static Settings plain() {
            return new Settings("shop.p12", false, false);
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    static Shop start(Settings settings) throws IOException {
        Path folder = Files.createTempDirectory("https-spring");
        Certificates.install(folder, "shop.p12", "shop2.p12", "client.p12", "impostor.p12", "trust.p12");
        int https = freePort();
        int http = freePort();
        String store = "file:" + folder.resolve(settings.serverKeystore());
        String trust = "file:" + folder.resolve("trust.p12");
        List<String> p = new ArrayList<>(List.of(
                "server.port=" + https,
                "server.address=127.0.0.1",
                "shop.http-port=" + http,
                "server.ssl.bundle=shop",
                "spring.ssl.bundle.jks.shop.keystore.location=" + store,
                "spring.ssl.bundle.jks.shop.keystore.password=" + Certificates.PASSWORD,
                "spring.ssl.bundle.jks.shop.keystore.type=PKCS12",
                "spring.ssl.bundle.jks.shop.options.enabled-protocols=TLSv1.3",
                "spring.ssl.bundle.jks.shop.reload-on-update=" + settings.reload(),
                "spring.ssl.bundle.watch.file.quiet-period=200ms",
                // client-side bundles: trust only the demo root; "ana" also holds a client certificate, "mallory" one signed by nobody
                "spring.ssl.bundle.jks.trust.truststore.location=" + trust,
                "spring.ssl.bundle.jks.trust.truststore.password=" + Certificates.PASSWORD,
                "spring.ssl.bundle.jks.trust.truststore.type=PKCS12",
                "spring.ssl.bundle.jks.ana.keystore.location=file:" + folder.resolve("client.p12"),
                "spring.ssl.bundle.jks.ana.keystore.password=" + Certificates.PASSWORD,
                "spring.ssl.bundle.jks.ana.keystore.type=PKCS12",
                "spring.ssl.bundle.jks.ana.truststore.location=" + trust,
                "spring.ssl.bundle.jks.ana.truststore.password=" + Certificates.PASSWORD,
                "spring.ssl.bundle.jks.ana.truststore.type=PKCS12",
                "spring.ssl.bundle.jks.mallory.keystore.location=file:" + folder.resolve("impostor.p12"),
                "spring.ssl.bundle.jks.mallory.keystore.password=" + Certificates.PASSWORD,
                "spring.ssl.bundle.jks.mallory.keystore.type=PKCS12",
                "spring.ssl.bundle.jks.mallory.truststore.location=" + trust,
                "spring.ssl.bundle.jks.mallory.truststore.password=" + Certificates.PASSWORD,
                "spring.ssl.bundle.jks.mallory.truststore.type=PKCS12",
                "server.servlet.session.cookie.secure=true",
                "server.servlet.session.cookie.http-only=true",
                "server.servlet.session.cookie.same-site=strict",
                "logging.level.root=OFF",
                "spring.main.banner-mode=off"));
        if (settings.requireClientCertificate()) {
            // the server's own bundle also needs the trust store, to check client certificates
            p.add("spring.ssl.bundle.jks.shop.truststore.location=" + trust);
            p.add("spring.ssl.bundle.jks.shop.truststore.password=" + Certificates.PASSWORD);
            p.add("spring.ssl.bundle.jks.shop.truststore.type=PKCS12");
            p.add("server.ssl.client-auth=need");
        }
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ShopApp.class).properties(p.toArray(String[]::new)).run();
        return new Shop(context, https, http, folder);
    }

    String https(String path) {
        return "https://" + Certificates.HOST + ":" + httpsPort + path;
    }

    String http(String path) {
        return "http://" + Certificates.HOST + ":" + httpPort + path;
    }

    /** The client-side context of one of the store's SSL bundles: "trust", "ana" or "mallory". */
    SSLContext context(String bundle) {
        return context.getBean(SslBundles.class).getBundle(bundle).createSslContext();
    }

    @Override
    public void close() throws IOException {
        context.close();
        try (var files = Files.list(folder)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
        Files.deleteIfExists(folder);
    }
}
