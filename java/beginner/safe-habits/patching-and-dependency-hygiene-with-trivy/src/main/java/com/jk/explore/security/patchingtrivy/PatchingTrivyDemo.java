package com.jk.explore.security.patchingtrivy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. It reads committed files in fixtures/ and needs no tool. */
public class PatchingTrivyDemo {

    static final String APPLICATION = "online-store";
    static final String APPLICATION_VERSION = "1.0.0";
    static final Path FIXTURES = Path.of("fixtures");

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("sbom")) {
            System.out.print(sbomFromLockfile());
            return;
        }
        System.out.print(run());
    }

    /** The bill of materials the Trivy scan reads: written from the lock file, so the two cannot disagree. */
    static String sbomFromLockfile() throws IOException {
        List<Dependency> locked = GradleLockfile.parse(Files.readString(FIXTURES.resolve("gradle.lockfile")));
        return CycloneDx.write(APPLICATION, APPLICATION_VERSION, locked);
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        act1Lockfile(out);
        act2Sbom(out);
        act3Report(out);
        act4Triage(out);
        act5Verification(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    private static String shortName(String packageName) {
        return packageName.substring(packageName.indexOf(':') + 1);
    }

    private static void act1Lockfile(StringBuilder out) throws IOException {
        line(out, "ONE. Pinning exactly what the build uses.");
        List<Dependency> locked = GradleLockfile.parse(Files.readString(FIXTURES.resolve("gradle.lockfile")));
        line(out, "  THE LOCK FILE (gradle.lockfile, " + locked.size() + " entries):");
        line(out, "    " + pad("group", 26) + pad("artifact", 16) + "version");
        for (Dependency d : locked) {
            line(out, "    " + pad(d.group(), 26) + pad(d.artifact(), 16) + d.version());
        }
        line(out, "  WHO ASKED FOR THEM:");
        line(out, "    the store's build asked for " + (locked.size() - 1) + " of the " + locked.size());
        line(out, "    commons-lang3 came on its own, with commons-text");
        line(out, "  BEFORE THE LOCK: a range such as 1.+ can change any day");
        line(out, "  AFTER THE LOCK: the same " + locked.size() + " versions on every build");
    }

    private static void act2Sbom(StringBuilder out) throws IOException {
        line(out, "TWO. A bill of materials a scanner can read.");
        String sbom = sbomFromLockfile();
        List<Dependency> back = CycloneDx.read(sbom);
        Dependency yaml = back.stream().filter(d -> d.artifact().equals("snakeyaml")).findFirst().orElseThrow();
        line(out, "  THE INPUT (one line of the lock file):");
        line(out, "    " + yaml.coordinates());
        line(out, "  THE OUTPUT (one of " + back.size() + " components, CycloneDX " + CycloneDx.SPEC_VERSION + "):");
        line(out, "    " + yaml.purl());
        line(out, "  THE PACKAGE URL, PART BY PART:");
        line(out, "    pkg:maven  " + pad(yaml.group(), 10) + pad(yaml.artifact(), 11) + yaml.version());
        line(out, "    type       " + pad("group", 10) + pad("name", 11) + "version");
        boolean same = back.equals(GradleLockfile.parse(Files.readString(FIXTURES.resolve("gradle.lockfile"))));
        line(out, "  THE CHECKS:");
        line(out, "    the SBOM lists the same " + back.size() + " as the lock file: " + same);
        line(out, "    online-store.cdx.json is what the code writes: "
                + sbom.equals(Files.readString(FIXTURES.resolve("online-store.cdx.json"))));
        String damaged = sbom.replace("\"version\": \"1.26\", \"purl\"", "\"version\": \"1.31\", \"purl\"");
        String result;
        try {
            CycloneDx.read(damaged);
            result = "accepted";
        } catch (IllegalArgumentException e) {
            result = "refused";
        }
        line(out, "    version 1.31 with a purl that says 1.26: " + result);
    }

    private static void act3Report(StringBuilder out) throws IOException {
        line(out, "THREE. What the scanner reported.");
        String json = Files.readString(FIXTURES.resolve("trivy-report.json"));
        List<Vulnerability> found = TrivyReport.parse(json);
        line(out, "  THE INPUT: online-store.cdx.json, scanned by Trivy " + TrivyReport.version(json));
        Map<String, Integer> counts = Triage.countBySeverity(found);
        line(out, "  THE REPORT (fixtures/trivy-report.json):");
        line(out, "    " + found.size() + " problems in " + found.stream().map(Vulnerability::packageName).distinct().count()
                + " libraries");
        line(out, "    CRITICAL " + counts.get("CRITICAL") + "   HIGH " + counts.get("HIGH") + "   MEDIUM " + counts.get("MEDIUM")
                + "   LOW " + counts.get("LOW"));
        Map<String, Long> perLibrary = new java.util.TreeMap<>();
        found.forEach(v -> perLibrary.merge(shortName(v.packageName()), 1L, Long::sum));
        perLibrary.forEach((name, n) -> line(out, "    " + pad(name, 15) + n + (n == 1 ? " problem" : " problems")));
        line(out, "  CROSS-CHECKS, BY THE STORE'S OWN CODE:");
        boolean allOlder = found.stream().allMatch(v -> v.nearestFix().isPresent() || v.fixed().isBlank());
        line(out, "    every installed version is older than its fix: " + allOlder);
        for (OwnAdvisory advisory : OwnAdvisory.known()) {
            boolean inReport = found.stream().anyMatch(v -> v.id().equals(advisory.id()));
            boolean ours = found.stream().anyMatch(v -> advisory.affects(v.packageName(), v.installed()));
            line(out, "    " + advisory.id() + "  our range check: " + ours + ", Trivy: " + inReport);
        }
        line(out, "  a clean report means no KNOWN problem, on the day it ran");
    }

    private static void act4Triage(StringBuilder out) throws IOException {
        line(out, "FOUR. What to fix first, and to which version.");
        List<Vulnerability> found = TrivyReport.parse(Files.readString(FIXTURES.resolve("trivy-report.json")));
        List<Vulnerability> ordered = Triage.ordered(found);
        line(out, "  THE MOST SERIOUS FIRST (5 of " + found.size() + "):");
        for (Vulnerability v : ordered.stream().limit(5).toList()) {
            line(out, "    " + pad(v.severity(), 9) + pad(v.id(), 16) + pad(shortName(v.packageName()) + " " + v.installed(), 19)
                    + (v.nearestFix().isPresent() ? "fix " + v.nearestFix().get() : "no fix yet"));
        }
        String log4j = "org.apache.logging.log4j:log4j-core";
        List<String> fixes = found.stream().filter(v -> v.packageName().equals(log4j))
                .flatMap(v -> v.nearestFix().stream()).distinct().sorted().map(Version::toString).toList();
        Map<String, Optional<Version>> targets = Triage.upgradeTargets(found);
        line(out, "  THE TARGET FOR log4j-core (the highest of its fixes):");
        line(out, "    fixes " + String.join(", ", fixes));
        line(out, "    highest " + targets.get(log4j).map(Version::toString).orElse("none") + ": one move fixes all "
                + found.stream().filter(v -> v.packageName().equals(log4j)).count());
        line(out, "  ONE MOVE PER LIBRARY:");
        for (Map.Entry<String, Optional<Version>> target : targets.entrySet()) {
            String installed = found.stream().filter(v -> v.packageName().equals(target.getKey())).findFirst().orElseThrow().installed();
            String to = target.getValue().map(Version::toString).orElse("no fix yet");
            boolean major = target.getValue().isPresent()
                    && target.getValue().get().numbers().get(0) > Version.parse(installed).numbers().get(0);
            line(out, "    " + pad(shortName(target.getKey()), 15) + pad(installed, 7) + "-> " + to
                    + (major ? "  <- NEW MAJOR: RUN THE TESTS" : ""));
        }
    }

    private static void act5Verification(StringBuilder out) throws IOException {
        line(out, "FIVE. Checking what is downloaded, and automating the updates.");
        Path file = FIXTURES.resolve("verified-build").resolve("gradle").resolve("verification-metadata.xml");
        String xml = Files.readString(file);
        Map<String, String> expected = VerificationMetadata.parse(xml);
        line(out, "  THE PINS (SHA-256 of " + expected.size() + " downloaded files):");
        for (Map.Entry<String, String> pin : expected.entrySet()) {
            String name = pin.getKey().substring(pin.getKey().indexOf('/') + 1);
            line(out, "    " + pad(name, 30) + pin.getValue().substring(0, 16) + "...");
        }
        String firstKey = expected.keySet().iterator().next();
        String tamperedXml = xml.replace(expected.get(firstKey), "0".repeat(64));
        List<String> different = VerificationMetadata.differences(expected, VerificationMetadata.parse(tamperedXml));
        String changed = different.get(0).substring(different.get(0).indexOf('/') + 1);
        line(out, "  BEFORE THE CHECK (a copy with one checksum changed):");
        line(out, "    " + pad(changed, 30) + "0".repeat(16) + "...");
        line(out, "  AFTER THE CHECK: " + different.size() + " file differs");
        line(out, "    " + changed + ": Gradle refuses to build");
        line(out, "  AUTOMATED UPDATES: dependabot.yml, renovate.json: "
                + (Files.exists(Path.of("infra", "dependabot.yml")) && Files.exists(Path.of("infra", "renovate.json"))));
        line(out, "    one pull request per update, so each review is small");
        line(out, "  the cost: every new hash needs a review,");
        line(out, "  and a scanner knows only what is in its database");
    }
}
