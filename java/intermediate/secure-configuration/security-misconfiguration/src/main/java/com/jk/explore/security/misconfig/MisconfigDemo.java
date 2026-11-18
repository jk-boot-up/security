package com.jk.explore.security.misconfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Runs the online store story in five numbered acts and prints exact, repeatable results. Each act prints its input (the settings
 * or the request) and its result (the reply or the verdict) in separate, labelled sections. Ports are random and the production
 * secret is never printed, so neither appears in the output.
 */
public class MisconfigDemo {

    /** A fixed demo session cookie, sent with the TRACE request to show that the reply echoes it back. */
    static final String COOKIE = "session=c0ffee42";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException, InsecureConfigException {
        StringBuilder out = new StringBuilder();
        Path dir = Files.createTempDirectory("misconfig-demo");
        try {
            int checks = act1Defaults(out, dir);
            act2Hardened(out, dir);
            act3Drift(out);
            act4Banners(out, dir);
            act5Bill(out, checks);
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

    /** A settings file as key=value lines, in a fixed order; the production secret is shown only by its length. */
    private static void settings(StringBuilder out, Properties p) {
        List<String> keys = new ArrayList<>(StoreConfig.KNOWN);
        new TreeSet<>(p.stringPropertyNames()).stream().filter(k -> !keys.contains(k)).forEach(keys::add);
        for (String key : keys) {
            if (p.containsKey(key)) {
                line(out, "    " + key + "=" + shown(p.getProperty(key)));
            }
        }
    }

    private static String shown(String value) {
        return value.equals(Profiles.DEMO_PRODUCTION_SECRET) ? "<the " + value.length() + "-character production secret>" : value;
    }

    private static void checks(StringBuilder out, String who, List<ConfigChecker.Check> checks) {
        line(out, "  THE " + checks.size() + " CHECKS AGAINST THE " + who + " STORE (verdict, check, seen):");
        for (ConfigChecker.Check check : checks) {
            line(out, String.format("    %-4s  %-19s %s", check.passed() ? "pass" : "FAIL", check.name(), check.detail()));
        }
        line(out, "  THE SCORE:");
    }

    private static int act1Defaults(StringBuilder out, Path dir) throws IOException, InterruptedException, InsecureConfigException {
        line(out, "ONE. The store started with its shipped defaults.");
        line(out, "  THE SETTINGS FILE (as shipped):");
        settings(out, Profiles.shipped());
        StoreConfig config = StoreConfig.from(Profiles.shipped());
        Path file = dir.resolve("shipped.properties");
        ConfigFile.write(file, Profiles.shipped(), "rw-r--r--");
        try (StoreServer store = StoreServer.start(config)) {
            ConfigChecker checker = new ConfigChecker(store.port());
            line(out, "  THE REQUESTS (what a scanner tries first):");
            line(out, "    POST /admin/login  password=admin");
            line(out, "    GET /debug/config");
            line(out, "    GET /static/");
            line(out, "    GET /orders?id=abc");
            ConfigChecker.Reply login = checker.send("POST", "/admin/login", "password=admin");
            ConfigChecker.Reply debug = checker.send("GET", "/debug/config", null);
            ConfigChecker.Reply listing = checker.send("GET", "/static/", null);
            ConfigChecker.Reply error = checker.send("GET", "/orders?id=abc", null);
            line(out, "  THE REPLIES (the status, and what the page shows):");
            line(out, "    " + login.status() + "  " + login.body() + "  <- ANYONE IS ADMIN");
            line(out, "    " + debug.status() + "  " + lineWith(debug.body(), "admin.password=") + "  <- THE SECRET");
            line(out, "    " + listing.status() + "  " + String.join(" ", listing.body().lines().skip(1).toList()));
            String exception = lineWith(error.body(), "Exception");
            int colon = exception.indexOf(": ");
            line(out, "    " + error.status() + "  " + exception.substring(exception.lastIndexOf('>', colon) + 1, colon + 1));
            line(out, "           " + exception.substring(colon + 2) + "  <- A STACK TRACE");
                List<ConfigChecker.Check> checks = checker.checkAll(config, file);
            checks(out, "SHIPPED", checks);
            line(out, "    " + ConfigChecker.score(checks) + " of " + checks.size() + "  <- EVERY DEFAULT IS A FINDING");
            return checks.size();
        }
    }

    private static String lineWith(String body, String part) {
        return body.lines().filter(l -> l.contains(part)).findFirst().orElse("");
    }

    private static void act2Hardened(StringBuilder out, Path dir) throws IOException, InterruptedException {
        line(out, "TWO. Secure defaults, and a gate at startup.");
        line(out, "  THE SETTINGS FILE (hardened: every setting stated):");
        settings(out, Profiles.hardened());
        StoreConfig config = StoreConfig.from(Profiles.hardened());
        Path file = dir.resolve("prod.properties");
        ConfigFile.write(file, Profiles.hardened(), "rw-------");
        try (StoreServer store = StoreServer.start(config)) {
            List<ConfigChecker.Check> checks = new ConfigChecker(store.port()).checkAll(config, file);
            checks(out, "HARDENED", checks);
            line(out, "    " + ConfigChecker.score(checks) + " of " + checks.size());
        } catch (InsecureConfigException e) {
            line(out, "  unexpected: " + e.getMessage());
        }
        line(out, "  THE GATE'S RULES (profile prod, before the port opens):");
        line(out, "    admin.password: " + Secrets.MIN_LENGTH + "+ characters, not empty, none of:");
        line(out, "      " + String.join(", ", Secrets.KNOWN_DEFAULTS));
        line(out, "    debug.endpoint, errors.detailed, http.trace: false; no test.*");
        line(out, "  THE INPUTS AND RESULTS (admin.password set to):");
        for (String password : List.of("admin", "", "Summer2026", Profiles.DEMO_PRODUCTION_SECRET)) {
            Properties p = Profiles.hardened();
            p.setProperty(StoreConfig.ADMIN_PASSWORD, password);
            line(out, "    " + (password.isEmpty() ? "(empty)" : shown(password)));
            line(out, "      -> " + startOrRefuse(StoreConfig.from(p)));
        }
    }

    private static String startOrRefuse(StoreConfig config) throws IOException {
        try (StoreServer ignored = StoreServer.start(config)) {
            return "started";
        } catch (InsecureConfigException e) {
            return "refused: " + e.getMessage();
        }
    }

    private static void act3Drift(StringBuilder out) throws IOException {
        line(out, "THREE. Drift: development settings that reach production.");
        Properties dev = Profiles.shipped();
        Properties prod = Profiles.copiedFromDevelopment();
        Properties changed = new Properties();
        for (String key : prod.stringPropertyNames()) {
            if (!prod.getProperty(key).equals(dev.getProperty(key))) {
                changed.setProperty(key, prod.getProperty(key));
            }
        }
        line(out, "  THE INPUT (development copied, " + changed.size() + " settings changed):");
        settings(out, changed);
        ProfileDrift.Report report = ProfileDrift.compare(dev, Profiles.hardened(), prod);
        line(out, "  THE CHECK, SETTING BY SETTING (must differ from dev):");
        for (String key : ProfileDrift.MUST_DIFFER) {
            line(out, String.format("    %-16s %s", key, report.stillDevelopment().contains(key)
                    ? dev.getProperty(key) + " in both  <- STILL DEVELOPMENT" : "changed"));
        }
        line(out, "  THE CHECK, KEY BY KEY (test.* = development only):");
        for (String key : report.developmentOnly()) {
            line(out, "    " + key + "  <- DEVELOPMENT ONLY");
        }
        line(out, "  THE RESULT:");
        line(out, "    drift found: " + !report.clean() + ", so the deployment fails");
        line(out, "    if it were deployed anyway, the gate:");
        line(out, "      -> " + startOrRefuse(StoreConfig.from(prod)));
        line(out, "    the hardened profile, same check: drift found "
                + !ProfileDrift.compare(dev, Profiles.hardened(), Profiles.hardened()).clean());
    }

    private static void act4Banners(StringBuilder out, Path dir) throws IOException, InterruptedException, InsecureConfigException {
        line(out, "FOUR. Banners, methods and file permissions.");
        try (StoreServer shipped = StoreServer.start(StoreConfig.from(Profiles.shipped()));
                StoreServer hardened = StoreServer.start(StoreConfig.from(Profiles.hardened()))) {
            ConfigChecker a = new ConfigChecker(shipped.port());
            ConfigChecker b = new ConfigChecker(hardened.port());
            line(out, "  THE REQUEST (to both stores):");
            line(out, "    GET /");
            line(out, "  THE REPLY'S Server HEADER:");
            line(out, "    shipped   " + a.send("GET", "/", null).server().orElse("(none)") + "  <- NAMES THE VERSION");
            line(out, "    hardened  " + b.send("GET", "/", null).server().orElse("(none)"));
            line(out, "  THE REQUEST (with the customer's session cookie):");
            line(out, "    TRACE /   Cookie: " + COOKIE);
            ConfigChecker.Reply open = a.send("TRACE", "/", null, "Cookie", COOKIE);
            ConfigChecker.Reply shut = b.send("TRACE", "/", null, "Cookie", COOKIE);
            line(out, "  THE REPLY (the status, and the body):");
            line(out, "    shipped   " + open.status() + "  " + lineWith(open.body(), "Cookie:") + "  <- ECHOED BACK");
            line(out, "    hardened  " + shut.status() + "  " + shut.body());
        }
        line(out, "  THE SETTINGS FILE ON DISK (owner, group, others):");
        for (String mode : List.of("rw-r--r--", "rw-------")) {
            Path file = dir.resolve(mode.replace('-', '_') + ".properties");
            ConfigFile.write(file, Profiles.hardened(), mode);
            String written = ConfigFile.permissions(file);
            line(out, "    " + written + "  =  " + written.substring(0, 3) + "  " + written.substring(3, 6) + "  " + written.substring(6));
            line(out, "      -> other users can read the admin secret: " + ConfigFile.readableByOthers(file));
        }
    }

    private static void act5Bill(StringBuilder out, int checks) {
        line(out, "FIVE. The bill: a baseline that must be checked again and again.");
        Properties prod = Profiles.hardened();
        prod.setProperty("uploads.max-size", "unlimited");
        ProfileDrift.Report report = ProfileDrift.compare(Profiles.shipped(), Profiles.hardened(), prod);
        line(out, "  THE INPUT (a new feature adds a setting):");
        for (String key : report.unreviewed()) {
            line(out, "    " + key + "=" + prod.getProperty(key));
        }
        line(out, "  THE DRIFT CHECK:");
        line(out, "    not in the baseline -> flagged for review: " + !report.clean());
        line(out, "  THE BILL:");
        line(out, "    every feature adds settings, each with an insecure value,");
        line(out, "    so the baseline grows with the store");
        line(out, "    the checks run in CI, at startup and on a schedule");
        line(out, "    passing means these " + checks + " checks pass, not that the store is safe");
    }
}
