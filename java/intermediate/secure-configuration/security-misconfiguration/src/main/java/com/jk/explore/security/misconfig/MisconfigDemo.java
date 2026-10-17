package com.jk.explore.security.misconfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. Ports are random, so they are never printed. */
public class MisconfigDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException, InsecureConfigException {
        StringBuilder out = new StringBuilder();
        Path dir = Files.createTempDirectory("misconfig-demo");
        try {
            act1Defaults(out, dir);
            act2Hardened(out, dir);
            act3Drift(out);
            act4Banners(out, dir);
            act5Bill(out);
        } finally {
            try (var files = Files.list(dir)) {
                for (Path p : files.toList()) {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(dir);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Defaults(StringBuilder out, Path dir) throws IOException, InterruptedException, InsecureConfigException {
        line(out, "ONE. The store started with its shipped defaults.");
        StoreConfig config = StoreConfig.from(Profiles.shipped());
        Path file = dir.resolve("shipped.properties");
        ConfigFile.write(file, Profiles.shipped(), "rw-r--r--");
        try (StoreServer store = StoreServer.start(config)) {
            ConfigChecker checker = new ConfigChecker(store.port());
            line(out, "  admin login with the default password admin: " + checker.send("POST", "/admin/login", "password=admin").status()
                    + "  <- ANYONE CAN LOG IN AS ADMIN");
            ConfigChecker.Reply debug = checker.send("GET", "/debug/config", null);
            line(out, "  GET /debug/config: " + debug.status() + ", and the page shows the admin password: " + debug.body().contains("admin.password="));
            ConfigChecker.Reply listing = checker.send("GET", "/static/", null);
            line(out, "  GET /static/: " + listing.status() + ", a list of " + StoreServer.STATIC_FILES.size()
                    + " files, including backup-orders.csv: " + listing.body().contains("backup-orders.csv"));
            ConfigChecker.Reply error = checker.send("GET", "/orders?id=abc", null);
            line(out, "  GET /orders?id=abc: " + error.status() + ", and the page shows a stack trace: " + error.body().contains("NumberFormatException"));
            List<ConfigChecker.Check> checks = checker.checkAll(config, file);
            line(out, "  the checker scores the shipped store " + ConfigChecker.score(checks) + " of " + checks.size()
                    + "  <- EVERY DEFAULT IS A FINDING");
        }
    }

    private static void act2Hardened(StringBuilder out, Path dir) throws IOException, InterruptedException {
        line(out, "TWO. Secure defaults, and a gate at startup.");
        StoreConfig config = StoreConfig.from(Profiles.hardened());
        Path file = dir.resolve("prod.properties");
        ConfigFile.write(file, Profiles.hardened(), "rw-------");
        try (StoreServer store = StoreServer.start(config)) {
            List<ConfigChecker.Check> checks = new ConfigChecker(store.port()).checkAll(config, file);
            for (ConfigChecker.Check check : checks) {
                line(out, "  " + check.name() + ": " + (check.passed() ? "pass" : "FAIL") + " (" + check.detail() + ")");
            }
            line(out, "  the checker scores the hardened store " + ConfigChecker.score(checks) + " of " + checks.size());
        } catch (InsecureConfigException e) {
            line(out, "  unexpected: " + e.getMessage());
        }
        Properties again = Profiles.hardened();
        again.setProperty(StoreConfig.ADMIN_PASSWORD, "admin");
        line(out, "  the hardened profile with the admin password set back to admin: " + startOrRefuse(StoreConfig.from(again)));
        again.setProperty(StoreConfig.ADMIN_PASSWORD, "");
        line(out, "  with an empty admin password: " + startOrRefuse(StoreConfig.from(again)));
    }

    private static String startOrRefuse(StoreConfig config) throws IOException {
        try (StoreServer ignored = StoreServer.start(config)) {
            return "started";
        } catch (InsecureConfigException e) {
            return "refused to start (" + e.getMessage() + ")";
        }
    }

    private static void act3Drift(StringBuilder out) throws IOException {
        line(out, "THREE. Drift: development settings that reach production.");
        Properties prod = Profiles.copiedFromDevelopment();
        line(out, "  production made by copying the development file and changing 5 settings");
        ProfileDrift.Report report = ProfileDrift.compare(Profiles.shipped(), Profiles.hardened(), prod);
        line(out, "  settings that still have their development value: " + String.join(", ", report.stillDevelopment()));
        line(out, "  development-only settings present in production: " + String.join(", ", report.developmentOnly()));
        line(out, "  the drift check fails the deployment: " + !report.clean());
        line(out, "  and if it were deployed anyway, the store: " + startOrRefuse(StoreConfig.from(prod)));
        line(out, "  the hardened profile compared the same way: drift found " + !ProfileDrift.compare(Profiles.shipped(), Profiles.hardened(),
                Profiles.hardened()).clean());
    }

    private static void act4Banners(StringBuilder out, Path dir) throws IOException, InterruptedException, InsecureConfigException {
        line(out, "FOUR. Banners, methods and file permissions.");
        try (StoreServer shipped = StoreServer.start(StoreConfig.from(Profiles.shipped()));
                StoreServer hardened = StoreServer.start(StoreConfig.from(Profiles.hardened()))) {
            ConfigChecker a = new ConfigChecker(shipped.port());
            ConfigChecker b = new ConfigChecker(hardened.port());
            line(out, "  Server header, shipped: " + a.send("GET", "/", null).server().orElse("none")
                    + "  <- TELLS AN ATTACKER WHICH ADVISORIES TO LOOK UP");
            line(out, "  Server header, hardened: " + b.send("GET", "/", null).server().orElse("none"));
            ConfigChecker.Reply trace = a.send("TRACE", "/", null);
            line(out, "  TRACE /, shipped: " + trace.status() + ", and the reply echoes the request headers back: " + trace.body().contains("TRACE /"));
            line(out, "  TRACE /, hardened: " + b.send("TRACE", "/", null).status() + ", the method is refused");
        }
        Path loose = dir.resolve("loose.properties");
        ConfigFile.write(loose, Profiles.hardened(), "rw-r--r--");
        line(out, "  the settings file written with " + ConfigFile.permissions(loose) + ": readable by other users on the machine: "
                + ConfigFile.readableByOthers(loose) + "  <- IT HOLDS THE ADMIN SECRET");
        Path tight = dir.resolve("tight.properties");
        ConfigFile.write(tight, Profiles.hardened(), "rw-------");
        line(out, "  written with " + ConfigFile.permissions(tight) + ": readable by other users: " + ConfigFile.readableByOthers(tight));
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: a baseline that must be checked again and again.");
        Properties prod = Profiles.hardened();
        prod.setProperty("uploads.max-size", "unlimited");
        ProfileDrift.Report report = ProfileDrift.compare(Profiles.shipped(), Profiles.hardened(), prod);
        line(out, "  a new feature adds a setting the baseline has never seen: " + String.join(", ", report.unreviewed())
                + ", flagged for review: " + !report.clean());
        line(out, "  every feature adds settings, and every setting has an insecure value, so the baseline grows with the store");
        line(out, "  the same checks belong in CI on every change, at every startup, and against the live store on a schedule");
        line(out, "  a passing score means these " + 8 + " checks pass, not that the store is secure");
    }
}
