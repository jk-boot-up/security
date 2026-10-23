package com.jk.explore.security.tlsattacks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** testssl.sh, pinned to a release tag and cloned into build/, run for real against a local server; its JSON is read here. */
final class TestSsl {

    static final String TAG = "v3.2.4";
    static final String COMMIT_PREFIX = "97763a4";
    static final String REPO = "https://github.com/testssl/testssl.sh.git";

    /** One finding of the report. */
    record Finding(String id, String severity, String text) {
    }

    private TestSsl() {
    }

    /** Clones the pinned tag once and checks that it is the commit that was reviewed. */
    static Path install(Path buildDir) throws IOException, InterruptedException {
        Path dir = buildDir.resolve("testssl.sh-" + TAG);
        if (!Files.exists(dir.resolve("testssl.sh"))) {
            Files.createDirectories(buildDir);
            Sh.must(List.of("git", "clone", "--quiet", "--depth", "1", "--branch", TAG, REPO, dir.toString()));
        }
        String head = Sh.must(List.of("git", "-C", dir.toString(), "rev-parse", "HEAD")).trim();
        if (!head.startsWith(COMMIT_PREFIX)) {
            throw new IllegalStateException("testssl.sh " + TAG + " is not the reviewed commit: " + head);
        }
        return dir.resolve("testssl.sh");
    }

    /** Runs the protocol, per-protocol cipher, LOGJAM and LUCKY13 checks against 127.0.0.1:port and returns the findings. */
    static List<Finding> scan(Path script, Openssl openssl, Path json, int port) throws IOException, InterruptedException {
        Files.deleteIfExists(json);
        Sh.Result r = Sh.run(null, java.util.Map.of(), "", List.of(script.toString(), "--openssl", openssl.path(), "--warnings", "off",
                "--color", "0", "--quiet", "-p", "-E", "-J", "-L", "--jsonfile", json.toString(), "127.0.0.1:" + port));
        if (!Files.exists(json)) {
            throw new IllegalStateException("testssl.sh wrote no report: " + r.output());
        }
        return parse(Files.readString(json));
    }

    static List<Finding> parse(String json) {
        List<Finding> out = new ArrayList<>();
        for (JsonNode n : new JsonMapper().readTree(json)) {
            out.add(new Finding(n.path("id").asString(), n.path("severity").asString(), n.path("finding").asString()));
        }
        return out;
    }

    /** Which planted weaknesses the report names. The static RSA and DH size ones need the key exchange column of the cipher table. */
    static Set<Weakness> detect(List<Finding> findings) {
        Set<Weakness> found = EnumSet.noneOf(Weakness.class);
        for (Finding f : findings) {
            if (f.id().equals("TLS1") && f.text().startsWith("offered")) {
                found.add(Weakness.TLS_1_0);
            } else if (f.id().equals("TLS1_1") && f.text().startsWith("offered")) {
                found.add(Weakness.TLS_1_1);
            } else if (f.id().startsWith("cipher-") && f.severity().equals("CRITICAL") && f.text().contains(" NULL-")) {
                found.add(Weakness.NULL_CIPHER);
            } else if (f.id().equals("LUCKY13") && f.text().startsWith("potentially vulnerable")) {
                found.add(Weakness.CBC_SHA1);
            }
        }
        if (!kx(findings, "RSA").isEmpty()) {
            found.add(Weakness.STATIC_RSA);
        }
        if (smallDh(findings)) {
            found.add(Weakness.SMALL_DH);
        }
        return found;
    }

    /** The cipher table rows (ids that start with cipher-) whose key exchange column equals kx. */
    static List<String> kx(List<Finding> findings, String kx) {
        List<String> rows = new ArrayList<>();
        for (Finding f : findings) {
            if (f.id().startsWith("cipher-")) {
                String[] t = f.text().trim().split("\\s+");
                if (t.length > 3 && t[3].equals(kx)) {
                    rows.add(t[2]);
                }
            }
        }
        return rows;
    }

    private static boolean smallDh(List<Finding> findings) {
        for (Finding f : findings) {
            if (f.id().startsWith("cipher-")) {
                String[] t = f.text().trim().split("\\s+");
                if (t.length > 4 && t[3].equals("DH") && t[4].matches("\\d+") && Integer.parseInt(t[4]) < 2048) {
                    return true;
                }
            }
        }
        return false;
    }

    /** How many findings have this severity or worse (LOW, MEDIUM, HIGH, CRITICAL). */
    static long atLeastLow(List<Finding> findings) {
        return findings.stream().filter(f -> Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(f.severity())).count();
    }
}
