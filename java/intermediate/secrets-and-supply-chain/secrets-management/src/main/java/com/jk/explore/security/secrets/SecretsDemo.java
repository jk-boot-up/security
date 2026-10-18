package com.jk.explore.security.secrets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. Secret values are never printed. */
public class SecretsDemo {

    /** Obviously fake demo values. Real secrets never appear in source code, which is the point of act one. */
    static final String DEMO_DB_PASSWORD = "not-a-real-password-demo";
    static final String DEMO_PAYMENT_KEY = "demo-key-not-real-0000";
    static final long LEASE_TTL = 900;
    static final long LEASE_MAX = 3600;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Path dir = Files.createTempDirectory("secrets-demo");
        try {
            act1WhereSecretsLeak(out, dir);
            act2RunTimeInjection(out, dir);
            act3LeasedLogins(out);
            act4Rotation(out);
            act5Bill(out);
        } finally {
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A random-looking token made at run time from a fixed seed, so no such string sits in the source code. */
    static String randomLookingToken() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        Random seeded = new Random(20261018L);
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            token.append(alphabet.charAt(seeded.nextInt(alphabet.length())));
        }
        return token.toString();
    }

    static Path writeSampleRepository(Path dir) throws IOException {
        Path repo = dir.resolve("shop-repo");
        Files.createDirectories(repo.resolve("config"));
        Files.createDirectories(repo.resolve("scripts"));
        Files.writeString(repo.resolve("config/application.properties"),
                "server.port=8443\ndb.url=jdbc:postgresql://db.internal/shop\ndb.password=" + DEMO_DB_PASSWORD + "\n"
                        + "payment.api-key=" + DEMO_PAYMENT_KEY + "\n", StandardCharsets.UTF_8);
        Files.writeString(repo.resolve("config/production.properties"),
                "db.password=${DB_PASSWORD}\npayment.api-key=file:/run/secrets/payment-key\n", StandardCharsets.UTF_8);
        Files.writeString(repo.resolve("scripts/refund.sh"),
                "#!/bin/sh\ncurl -H \"X-Auth: " + randomLookingToken() + "\" https://payments.example/refunds\n", StandardCharsets.UTF_8);
        return repo;
    }

    private static void act1WhereSecretsLeak(StringBuilder out, Path dir) throws IOException, InterruptedException {
        line(out, "ONE. Where secrets leak.");
        Path repo = writeSampleRepository(dir);
        List<SecretScanner.Finding> findings = SecretScanner.scan(repo);
        line(out, "  the scanner reads the 3 files the team committed and finds " + findings.size() + " secrets:");
        for (SecretScanner.Finding finding : findings) {
            line(out, "    " + finding);
        }
        line(out, "  production.properties names where its secrets come from, ${DB_PASSWORD} and a file, so it holds none");

        ProcessBuilder child = new ProcessBuilder("printenv", "SHOP_PAYMENT_KEY").redirectErrorStream(true);
        child.environment().put("SHOP_PAYMENT_KEY", DEMO_PAYMENT_KEY);
        Process process = child.start();
        String seen = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        process.waitFor();
        line(out, "  a key in an environment variable is read by any program the store starts: " + seen.equals(DEMO_PAYMENT_KEY)
                + "  <- AND BY CRASH REPORTS AND DEBUG PAGES THAT PRINT THE ENVIRONMENT");

        ImageLayers image = new ImageLayers();
        image.addLayer(Map.of("app/shop.jar", "the store"));
        image.addLayer(Map.of("app/config/payment.key", DEMO_PAYMENT_KEY));
        image.deleteInNewLayer("app/config/payment.key");
        boolean running = image.mergedView().containsKey("app/config/payment.key");
        boolean inLayer = image.layer(1).containsValue(DEMO_PAYMENT_KEY);
        line(out, "  an image built with the key, which a later layer deletes: " + image.layerCount() + " layers");
        line(out, "    the running container sees the key: " + running);
        line(out, "    anyone with the image file reads it from layer two: " + inLayer + "  <- DELETING IN A LATER LAYER HIDES, IT DOES NOT REMOVE");
    }

    private static void act2RunTimeInjection(StringBuilder out, Path dir) throws IOException {
        line(out, "TWO. Secrets injected at run time.");
        Path mount = Files.createDirectories(dir.resolve("run-secrets"));
        Path loose = mount.resolve("db-password");
        MountedSecrets.write(loose, DEMO_DB_PASSWORD + "\n", "rw-r--r--");
        try {
            MountedSecrets.read(loose);
            line(out, "  a secret file with permissions rw-r--r--: read");
        } catch (MountedSecrets.UnsafeFileException e) {
            line(out, "  a secret file with permissions rw-r--r--: refused at startup, because others can read it");
        }
        MountedSecrets.write(loose, DEMO_DB_PASSWORD + "\n", "rw-------");
        Secret password;
        try {
            password = MountedSecrets.read(loose);
        } catch (MountedSecrets.UnsafeFileException e) {
            throw new IllegalStateException(e);
        }
        line(out, "  the same file with permissions " + MountedSecrets.permissions(loose) + ": read once at startup, "
                + password.length() + " characters, nothing in the code or the image");
        line(out, "  the secret written into a log line: \"connecting with password " + password + "\"");
        Secret copyForPool = Secret.of(password.use(char[]::clone));
        password.wipe();
        line(out, "  once the connection pool has its copy, the startup copy is wiped: " + password.isWiped()
                + ", and using it now fails: " + !tryUse(password));
        copyForPool.wipe();
    }

    private static boolean tryUse(Secret secret) {
        try {
            secret.use(chars -> chars.length);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static void act3LeasedLogins(StringBuilder out) throws SecretsService.RefusedException {
        line(out, "THREE. A secrets service that hands out leased logins.");
        SimClock clock = new SimClock();
        Database database = new Database(clock);
        SecretsService service = new SecretsService(clock, database, LEASE_TTL, LEASE_MAX);
        Secret shopCredential = Secret.of("demo-shop-api-identity");
        Secret reportsCredential = Secret.of("demo-reports-identity");
        service.registerApp("shop-api", shopCredential);
        service.registerApp("reports", reportsCredential);

        SecretsService.Lease shop = service.leaseDatabaseLogin("shop-api", shopCredential);
        SecretsService.Lease reports = service.leaseDatabaseLogin("reports", reportsCredential);
        line(out, "  shop-api proves who it is and gets a database login of its own: " + shop.user() + ", for "
                + (shop.expiresAt() - shop.issuedAt()) + " seconds");
        line(out, "  reports gets a different login: " + reports.user() + "; no two applications share one: "
                + !shop.user().equals(reports.user()));
        try {
            service.leaseDatabaseLogin("shop-api", Secret.of("a-guess"));
            line(out, "  a wrong application credential: leased");
        } catch (SecretsService.RefusedException e) {
            line(out, "  a wrong application credential: refused");
        }

        clock.advance(600);
        line(out, "  at t=600 the shop's login works: " + database.login(shop.user(), shop.password()));
        shop = service.renew(shop.id());
        line(out, "  the shop renews its lease at t=600: now until t=" + shop.expiresAt());
        clock.advance(300);
        int ended = service.expireLeases();
        line(out, "  at t=900 the reports lease ends, it was never renewed: " + ended + " login removed, reports can log in: "
                + database.login(reports.user(), reports.password()));
        for (long t = 1400; t <= 3500; t += 700) {
            clock.advance(t - clock.now());
            shop = service.renew(shop.id());
        }
        line(out, "  renewing every few minutes, the lease stops at the maximum of " + LEASE_MAX + " seconds after issue: until t="
                + shop.expiresAt());
        clock.advance(LEASE_MAX - clock.now());
        service.expireLeases();
        line(out, "  at t=" + clock.now() + " the shop's lease has ended and the database refuses the login: "
                + !database.login(shop.user(), shop.password()) + ", so the shop asks for a new one");
        line(out, "  the audit trail records every request: " + service.audit().size() + " entries, the first: "
                + service.audit().get(0));
    }

    private static void act4Rotation(StringBuilder out) throws SecretsService.RefusedException {
        line(out, "FOUR. Rotation without downtime, and the blast radius of a leak.");
        PaymentProvider provider = new PaymentProvider();
        Secret v1 = Secret.of("demo-payment-key-v1");
        Secret v2 = Secret.of("demo-payment-key-v2");
        provider.issue("v1", v1);
        line(out, "  the store calls the payment provider with key v1: " + provider.accepts(v1));
        provider.issue("v2", v2);
        line(out, "  the provider issues v2, and during the changeover it accepts " + provider.activeVersions() + ": v1 "
                + provider.accepts(v1) + ", v2 " + provider.accepts(v2));
        line(out, "  every server of the store switches to v2, then v1 is revoked: v1 " + afterRevoke(provider, v1)
                + ", v2 " + provider.accepts(v2));

        SimClock clock = new SimClock();
        Database database = new Database(clock);
        database.createAccount("shop", Secret.of(DEMO_DB_PASSWORD), Database.NEVER);
        SecretsService service = new SecretsService(clock, database, LEASE_TTL, LEASE_MAX);
        Secret credential = Secret.of("demo-shop-api-identity");
        service.registerApp("shop-api", credential);
        SecretsService.Lease lease = service.leaseDatabaseLogin("shop-api", credential);
        clock.advance(600);
        line(out, "  a leased login copied at t=600 works for at most " + (lease.expiresAt() - clock.now())
                + " more seconds, or " + (LEASE_MAX - clock.now()) + " if the thief can renew it");
        service.revoke(lease.id());
        line(out, "  and once the leak is noticed, revoking the lease ends it at once: the login works: "
                + database.login(lease.user(), lease.password()));
        clock.advance(3L * 365 * 24 * 3600);
        line(out, "  a static database password copied at t=600 still works three years later, until someone remembers to change it: "
                + database.login("shop", Secret.of(DEMO_DB_PASSWORD)));
    }

    private static boolean afterRevoke(PaymentProvider provider, Secret v1) {
        provider.revoke("v1");
        return provider.accepts(v1);
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        line(out, "  the secrets service holds every secret, so it needs its own access control, audit and backups");
        line(out, "  the application still needs a first credential to prove who it is; platform identity, in the advanced track, removes it");
        line(out, "  leases and rotation need applications that can reconnect with a new login without a restart");
        line(out, "  a scanner finds only what it has patterns for, so it is one check, not a guarantee, and history must be scanned too");
        line(out, "  the -with-openbao version runs the same leases against a real secrets server");
    }
}
