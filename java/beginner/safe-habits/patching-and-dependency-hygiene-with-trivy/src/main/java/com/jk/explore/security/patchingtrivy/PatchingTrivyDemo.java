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

    private static void act1Lockfile(StringBuilder out) throws IOException {
        line(out, "ONE. Pinning exactly what the build uses.");
        List<Dependency> locked = GradleLockfile.parse(Files.readString(FIXTURES.resolve("gradle.lockfile")));
        line(out, "  Gradle's lock file, gradle.lockfile, records the exact version of every library: " + locked.size() + " entries");
        for (Dependency d : locked) {
            line(out, "    " + d.coordinates());
        }
        line(out, "  the store asked for 4 of these; commons-lang3 arrived on its own, as a dependency of commons-text");
        line(out, "  without the file, a range such as 1.+ would pick a different version on a different day");
    }

    private static void act2Sbom(StringBuilder out) throws IOException {
        line(out, "TWO. A bill of materials a scanner can read.");
        String sbom = sbomFromLockfile();
        List<Dependency> back = CycloneDx.read(sbom);
        line(out, "  the store writes a CycloneDX " + CycloneDx.SPEC_VERSION + " file listing " + back.size() + " libraries, each with a package URL");
        line(out, "  first package URL: " + back.get(0).purl());
        boolean same = back.equals(GradleLockfile.parse(Files.readString(FIXTURES.resolve("gradle.lockfile"))));
        line(out, "  the bill of materials and the lock file list the same libraries: " + same);
        line(out, "  the committed fixtures/online-store.cdx.json is exactly what the code writes: "
                + sbom.equals(Files.readString(FIXTURES.resolve("online-store.cdx.json"))));
        String damaged = sbom.replace("\"version\": \"1.26\", \"purl\"", "\"version\": \"1.31\", \"purl\"");
        String result;
        try {
            CycloneDx.read(damaged);
            result = "accepted";
        } catch (IllegalArgumentException e) {
            result = "refused (" + e.getMessage() + ")";
        }
        line(out, "  a package URL that disagrees with its version: " + result);
    }

    private static void act3Report(StringBuilder out) throws IOException {
        line(out, "THREE. What the scanner reported.");
        String json = Files.readString(FIXTURES.resolve("trivy-report.json"));
        List<Vulnerability> found = TrivyReport.parse(json);
        line(out, "  Trivy " + TrivyReport.version(json) + " scanned the bill of materials; the report is saved in fixtures/trivy-report.json");
        Map<String, Integer> counts = Triage.countBySeverity(found);
        line(out, "  it reported " + found.size() + " problems in " + found.stream().map(Vulnerability::packageName).distinct().count()
                + " libraries: " + counts.get("CRITICAL") + " critical, " + counts.get("HIGH") + " high, " + counts.get("MEDIUM") + " medium, "
                + counts.get("LOW") + " low");
        boolean allOlder = found.stream().allMatch(v -> v.nearestFix().isPresent() || v.fixed().isBlank());
        line(out, "  every installed version is older than the version that fixes it, as the code compares them: " + allOlder);
        for (OwnAdvisory advisory : OwnAdvisory.known()) {
            boolean inReport = found.stream().anyMatch(v -> v.id().equals(advisory.id()));
            boolean ours = found.stream().anyMatch(v -> advisory.affects(v.packageName(), v.installed()));
            line(out, "  " + advisory.id() + ": the store's own range check says affected: " + ours + ", Trivy reports it: " + inReport);
        }
        line(out, "  a scanner only knows what is in its database on the day it runs, so a clean report means no known problem today");
    }

    private static void act4Triage(StringBuilder out) throws IOException {
        line(out, "FOUR. What to fix first, and to which version.");
        List<Vulnerability> found = TrivyReport.parse(Files.readString(FIXTURES.resolve("trivy-report.json")));
        List<Vulnerability> ordered = Triage.ordered(found);
        line(out, "  the most serious first:");
        for (Vulnerability v : ordered.stream().limit(5).toList()) {
            line(out, "    " + v.severity() + " " + v.id() + " in " + v.packageName() + " " + v.installed()
                    + (v.nearestFix().isPresent() ? ", fixed in " + v.nearestFix().get() : ", no fix yet"));
        }
        line(out, "  one move per library fixes everything that has a fix:");
        for (Map.Entry<String, Optional<Version>> target : Triage.upgradeTargets(found).entrySet()) {
            String installed = found.stream().filter(v -> v.packageName().equals(target.getKey())).findFirst().orElseThrow().installed();
            line(out, "    " + target.getKey() + " " + installed + " -> "
                    + target.getValue().map(Version::toString).orElse("no fixed version exists yet"));
        }
        line(out, "  each target is the highest of the nearest fixes, so log4j-core needs 2.25.4 for its last problem, further than the first fix, 2.15.0, suggested");
        line(out, "  a fix that crosses a major version, such as snakeyaml 1.26 to 2.0, can break the build, so run the tests after each move");
    }

    private static void act5Verification(StringBuilder out) throws IOException {
        line(out, "FIVE. Checking what is downloaded, and automating the updates.");
        Path file = FIXTURES.resolve("verified-build").resolve("gradle").resolve("verification-metadata.xml");
        String xml = Files.readString(file);
        Map<String, String> expected = VerificationMetadata.parse(xml);
        line(out, "  Gradle's verification-metadata.xml pins the SHA-256 of " + expected.size() + " downloaded files");
        String firstKey = expected.keySet().iterator().next();
        String tamperedXml = xml.replace(expected.get(firstKey), "0".repeat(64));
        List<String> different = VerificationMetadata.differences(expected, VerificationMetadata.parse(tamperedXml));
        line(out, "  one checksum changed in a copy of the file: " + different.size() + " file differs, " + different.get(0));
        line(out, "  Gradle refuses to build when a download does not match (the integration test runs this with the real Gradle)");
        line(out, "  dependabot.yml and renovate.json in infra/ open a pull request for each update, so the review is small: "
                + (Files.exists(Path.of("infra", "dependabot.yml")) && Files.exists(Path.of("infra", "renovate.json"))));
        line(out, "  the cost: every update means reviewing a new hash, and a scanner is only as good as its database");
    }
}
