package com.jk.explore.security.secrets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

    private static String pad(String text, int width) {
        return text.length() >= width ? text + " " : text + " ".repeat(width - text.length());
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final String[] TIMES = {"none", "once", "twice", "three times", "four times"};

    /** The entropy sum, one group of characters per line, with the real numbers. */
    private static void entropySteps(StringBuilder out, String value) {
        int length = value.length();
        StringBuilder sum = new StringBuilder();
        for (Map.Entry<Integer, Integer> group : SecretScanner.charactersByCount(value).entrySet()) {
            int n = group.getKey();
            int k = group.getValue();
            double bits = k * ((double) n / length) * (Math.log((double) length / n) / Math.log(2));
            line(out, "    " + pad(k + (k == 1 ? " character " : " characters ") + TIMES[n], 26) + k + " x " + n + "/" + length + " x log2(" + length + "/" + n
                    + ") = " + num(bits));
            sum.append(sum.isEmpty() ? "" : " + ").append(num(bits));
        }
        double entropy = SecretScanner.entropy(value);
        boolean flagged = !SecretScanner.scanText("value", value).isEmpty();
        line(out, "    entropy  " + sum + " = " + num(entropy) + (flagged ? ", at least " : ", under ")
                + SecretScanner.ENTROPY_THRESHOLD + (flagged ? ": flagged" : ": not flagged"));
    }

    private static void act1WhereSecretsLeak(StringBuilder out, Path dir) throws IOException, InterruptedException {
        line(out, "ONE. Where secrets leak.");
        Path repo = writeSampleRepository(dir);
        List<Path> files;
        try (Stream<Path> walk = Files.walk(repo)) {
            files = walk.filter(Files::isRegularFile).sorted().toList();
        }
        line(out, "  THE FILES THE TEAM COMMITTED (" + files.size() + "):");
        for (Path file : files) {
            line(out, "    " + pad(repo.relativize(file).toString(), 31) + Files.readAllLines(file).size() + " lines");
        }
        List<SecretScanner.Finding> findings = SecretScanner.scan(repo);
        line(out, "  WHAT THE SCANNER FINDS (" + findings.size() + " secrets, values never printed):");
        for (SecretScanner.Finding finding : findings) {
            line(out, "    " + pad(finding.file() + ":" + finding.line(), 33) + finding.rule());
        }
        line(out, "  WHAT IT SKIPS (a placeholder says where a secret comes from):");
        Path production = repo.resolve("config/production.properties");
        List<String> settings = Files.readAllLines(production);
        for (int i = 0; i < settings.size(); i++) {
            String value = settings.get(i).substring(settings.get(i).indexOf('=') + 1);
            if (SecretScanner.isPlaceholder(value)) {
                line(out, "    " + pad(repo.relativize(production) + ":" + (i + 1), 33) + value);
            }
        }
        String token = randomLookingToken();
        line(out, "  HOW THE TOKEN'S ENTROPY IS WORKED OUT (" + token.length() + " characters, not shown):");
        entropySteps(out, token);
        String className = "ProductCatalogueController";
        line(out, "  THE SAME SUM FOR A CLASS NAME, " + className + ":");
        entropySteps(out, className);

        ProcessBuilder child = new ProcessBuilder("printenv", "SHOP_PAYMENT_KEY").redirectErrorStream(true);
        child.environment().put("SHOP_PAYMENT_KEY", DEMO_PAYMENT_KEY);
        Process process = child.start();
        String seen = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        process.waitFor();
        line(out, "  A KEY IN AN ENVIRONMENT VARIABLE:");
        line(out, "    the store sets  SHOP_PAYMENT_KEY, then starts printenv");
        line(out, "    printenv reads  the key: " + seen.equals(DEMO_PAYMENT_KEY));
        line(out, "    so does any crash report that prints the environment");

        ImageLayers image = new ImageLayers();
        image.addLayer(Map.of("app/shop.jar", "the store"));
        image.addLayer(Map.of("app/config/payment.key", DEMO_PAYMENT_KEY));
        image.deleteInNewLayer("app/config/payment.key");
        line(out, "  THE IMAGE, LAYER BY LAYER (" + image.layerCount() + " layers):");
        for (int i = 0; i < image.layerCount(); i++) {
            String name = image.layer(i).keySet().iterator().next();
            line(out, "    layer " + (i + 1) + "  " + name + (name.contains(ImageLayers.WHITEOUT) ? "  (hides the file)" : ""));
        }
        line(out, "  WHAT THE RUNNING CONTAINER SEES:");
        line(out, "    files " + image.mergedView().keySet() + ", the key: "
                + image.mergedView().containsKey("app/config/payment.key"));
        line(out, "  WHAT ANYONE WITH THE IMAGE FILE READS:");
        line(out, "    layer 2 still holds the key: " + image.layer(1).containsValue(DEMO_PAYMENT_KEY)
                + "  <- HIDDEN, NOT REMOVED");
    }

    private static void act2RunTimeInjection(StringBuilder out, Path dir) throws IOException {
        line(out, "TWO. Secrets injected at run time.");
        Path mount = Files.createDirectories(dir.resolve("run/secrets"));
        Path file = mount.resolve("db-password");
        String name = dir.relativize(file).toString();
        MountedSecrets.write(file, DEMO_DB_PASSWORD + "\n", "rw-r--r--");
        line(out, "  BEFORE THE FIX, the mounted file " + name + ":");
        line(out, "    permissions  " + MountedSecrets.permissions(file));
        line(out, "    by whom      " + MountedSecrets.byWhom(MountedSecrets.permissions(file)));
        try {
            MountedSecrets.read(file);
            line(out, "    at startup   read");
        } catch (MountedSecrets.UnsafeFileException e) {
            line(out, "    at startup   refused: group and others can read it");
        }
        MountedSecrets.write(file, DEMO_DB_PASSWORD + "\n", "rw-------");
        Secret password;
        try {
            password = MountedSecrets.read(file);
        } catch (MountedSecrets.UnsafeFileException e) {
            throw new IllegalStateException(e);
        }
        line(out, "  AFTER THE FIX, the same file:");
        line(out, "    permissions  " + MountedSecrets.permissions(file));
        line(out, "    by whom      " + MountedSecrets.byWhom(MountedSecrets.permissions(file)));
        line(out, "    at startup   read once, " + password.length() + " characters, not in the code or image");
        line(out, "  THE SECRET WRITTEN INTO A LOG LINE:");
        line(out, "    the code  \"connecting with password \" + password");
        line(out, "    the log   connecting with password " + password);
        Secret copyForPool = Secret.of(password.use(char[]::clone));
        password.wipe();
        line(out, "  ONCE THE CONNECTION POOL HAS ITS COPY:");
        line(out, "    the startup copy is wiped: " + password.isWiped() + ", using it fails: " + !tryUse(password));
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

    private static SecretsService.Lease renewAndShow(StringBuilder out, SecretsService service, SimClock clock,
                                                     SecretsService.Lease lease) throws SecretsService.RefusedException {
        SecretsService.Lease renewed = service.renew(lease.id());
        line(out, "    " + pad("t=" + clock.now(), 8) + "min(" + clock.now() + " + " + LEASE_TTL + ", " + lease.issuedAt() + " + "
                + LEASE_MAX + ") = min(" + (clock.now() + LEASE_TTL) + ", " + (lease.issuedAt() + LEASE_MAX) + ") = "
                + renewed.expiresAt());
        return renewed;
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

        line(out, "  THE REQUESTS (time to live " + LEASE_TTL + " s, maximum " + LEASE_MAX + " s):");
        SecretsService.Lease shop = service.leaseDatabaseLogin("shop-api", shopCredential);
        line(out, "    t=0  shop-api, its credential  " + shop.user() + " until t=" + shop.expiresAt());
        SecretsService.Lease reports = service.leaseDatabaseLogin("reports", reportsCredential);
        line(out, "    t=0  reports, its credential   " + reports.user() + " until t=" + reports.expiresAt());
        try {
            service.leaseDatabaseLogin("shop-api", Secret.of("a-guess"));
            line(out, "    t=0  shop-api, a wrong one     leased");
        } catch (SecretsService.RefusedException e) {
            line(out, "    t=0  shop-api, a wrong one     refused");
        }

        line(out, "  THE LEASES OVER TIME (a renewal: min(now + ttl, issued + max)):");
        clock.advance(600);
        line(out, "    t=600   " + shop.user() + " works: " + database.login(shop.user(), shop.password()));
        shop = renewAndShow(out, service, clock, shop);
        clock.advance(300);
        int ended = service.expireLeases();
        line(out, "    t=900   the reports lease ends, never renewed: " + ended + " login removed");
        line(out, "            " + reports.user() + " works: " + database.login(reports.user(), reports.password()));
        for (long t = 1400; t <= 3500; t += 700) {
            clock.advance(t - clock.now());
            shop = renewAndShow(out, service, clock, shop);
        }
        clock.advance(LEASE_MAX - clock.now());
        service.expireLeases();
        line(out, "  AT t=" + clock.now() + ", THE MAXIMUM:");
        line(out, "    " + shop.user() + " works: " + database.login(shop.user(), shop.password())
                + ", so the shop asks for a new lease");
        line(out, "  THE AUDIT TRAIL (" + service.audit().size() + " entries):");
        for (String entry : service.audit()) {
            line(out, "    " + entry);
        }
    }

    private static void act4Rotation(StringBuilder out) throws SecretsService.RefusedException {
        line(out, "FOUR. Rotation without downtime, and the blast radius of a leak.");
        PaymentProvider provider = new PaymentProvider();
        Secret v1 = Secret.of("demo-payment-key-v1");
        Secret v2 = Secret.of("demo-payment-key-v2");
        line(out, "  THE PAYMENT KEY, STEP BY STEP (what the provider accepts):");
        provider.issue("v1", v1);
        rotationStep(out, provider, "1 v1 in use", v1, v2);
        provider.issue("v2", v2);
        rotationStep(out, provider, "2 v2 issued", v1, v2);
        provider.revoke("v1");
        rotationStep(out, provider, "3 all on v2, v1 revoked", v1, v2);

        SimClock clock = new SimClock();
        Database database = new Database(clock);
        database.createAccount("shop", Secret.of(DEMO_DB_PASSWORD), Database.NEVER);
        SecretsService service = new SecretsService(clock, database, LEASE_TTL, LEASE_MAX);
        Secret credential = Secret.of("demo-shop-api-identity");
        service.registerApp("shop-api", credential);
        SecretsService.Lease lease = service.leaseDatabaseLogin("shop-api", credential);
        clock.advance(600);
        long copied = clock.now();
        line(out, "  A LOGIN A THIEF COPIES AT t=" + copied + ":");
        line(out, "    leased   ends at t=" + lease.expiresAt() + ": " + lease.expiresAt() + " - " + copied + " = "
                + (lease.expiresAt() - copied) + " s more");
        line(out, "    renewed  at most t=" + (lease.issuedAt() + LEASE_MAX) + ": " + (lease.issuedAt() + LEASE_MAX) + " - " + copied
                + " = " + (lease.issuedAt() + LEASE_MAX - copied) + " s more");
        service.revoke(lease.id());
        line(out, "    revoked  the login works right after: " + database.login(lease.user(), lease.password()));
        clock.advance(3L * 365 * 24 * 3600);
        line(out, "  A STATIC PASSWORD THE THIEF COPIES AT THE SAME TIME:");
        line(out, "    three years later it works: " + database.login("shop", Secret.of(DEMO_DB_PASSWORD))
                + "  <- UNTIL SOMEONE CHANGES IT");
    }

    private static void rotationStep(StringBuilder out, PaymentProvider provider, String step, Secret v1, Secret v2) {
        line(out, "    " + pad(step, 25) + pad(provider.activeVersions().toString(), 10) + "v1 " + pad(String.valueOf(provider.accepts(v1)), 6)
                + "v2 " + provider.accepts(v2));
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        line(out, "  WHAT IT COSTS:");
        line(out, "    the secrets service holds every secret: guard, audit, back up");
        line(out, "    the first credential remains: platform identity removes it");
        line(out, "    applications must take a new login without a restart");
        line(out, "    a scanner finds only its patterns: scan history and images");
        line(out, "  NEXT:");
        line(out, "    the -with-openbao version runs the leases on a real server");
    }
}
