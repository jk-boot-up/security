package com.jk.explore.security.sbomscan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runs the store's bill-of-materials story in five numbered acts with the real syft and grype commands on images built on this machine. */
public class SbomScanDemo {

    static final String CORE = "pkg:maven/org.apache.logging.log4j/log4j-core@";
    static final String API = "pkg:maven/org.apache.logging.log4j/log4j-api@";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Counts that depend on the vulnerability database of the day are never printed. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        Lab lab = new Lab();
        String old = lab.build(Lab.OLD, Lab.OLD, null);
        String patched = lab.build("2.16.0", "2.16.0", null);
        String current = lab.build(Lab.NEW, Lab.NEW, null);
        String renamed = lab.build("renamed", Lab.OLD, "blob.dat");
        Sbom sbom = Sbom.of(old);
        ensureFreshDatabase();
        Scan scan = Scan.ofImage(old);
        act1WhatItAnswers(out, lab, sbom);
        act2Scan(out, lab, sbom, scan);
        Sbom newer = Sbom.of(current);
        Scan newerScan = Scan.ofImage(current);
        act3Upgrade(out, sbom, scan, newer, newerScan);
        act4Gate(out, scan, Scan.ofImage(patched), newerScan);
        act5Bill(out, sbom, renamed);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A labelled row: the label padded to a column (at least two spaces), then the value. */
    private static void row(StringBuilder out, int width, String label, String value) {
        line(out, "    " + String.format("%-" + Math.max(width, label.length() + 2) + "s", label) + value);
    }

    private static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

    private static String shortNames(java.util.Collection<String> purls) {
        return String.join(", ", purls.stream().map(p -> Purl.parse(p).shortName()).toList());
    }

    private static void act1WhatItAnswers(StringBuilder out, Lab lab, Sbom sbom) throws IOException, InterruptedException {
        line(out, "ONE. What a bill of materials answers, and what it does not.");
        line(out, "  THE IMAGE (the input):");
        row(out, 10, "image", "shop-sbom:" + Lab.OLD);
        row(out, 10, "base", Lab.BASE);
        row(out, 10, "/app/lib", "log4j-core-" + Lab.OLD + ".jar, log4j-api-" + Lab.OLD + ".jar");
        row(out, 10, "command", "syft shop-sbom:" + Lab.OLD + " -o cyclonedx-json");
        line(out, "  THE BILL OF MATERIALS (CycloneDX " + sbom.specVersion() + ", the result):");
        row(out, 13, "subject", sbom.subject());
        row(out, 13, "OS packages", "listed, as pkg:apk/... (" + yesNo(!sbom.osPackages().isEmpty()) + ")");
        row(out, 13, "libraries", shortNames(sbom.withName("log4j")));
        String core = CORE + Lab.OLD;
        Purl purl = Purl.parse(core);
        line(out, "  ONE PACKAGE URL, PART BY PART:");
        line(out, "    " + core);
        row(out, 11, "type", purl.type());
        row(out, 11, "namespace", purl.namespace());
        row(out, 11, "name", purl.name());
        row(out, 11, "version", purl.version());
        String path = "/app/lib/log4j-core-" + Lab.OLD + ".jar";
        String recorded = sbom.fileHash(path, "SHA-1");
        String computed = lab.sha1("log4j-core", Lab.OLD);
        line(out, "  ITS FILE'S FINGERPRINT, CHECKED:");
        row(out, 13, "the file", path);
        row(out, 13, "in the bill", "SHA-1 " + recorded);
        row(out, 13, "of the jar", "SHA-1 " + computed);
        row(out, 13, "the same", yesNo(recorded.equals(computed)));
        row(out, 13, "licence", "Apache-2.0, named in the bill: " + yesNo(sbom.mentions("Apache-2.0")));
        line(out, "  WHAT THE BILL DOES NOT SAY:");
        row(out, 28, "repository, commit, builder", (sbom.document().containsKey("vcs") ? "in it" : "not in it") + "  <- PROVENANCE");
    }

    /** One advisory against the store's log4j jars, merged over the packages it was found in. */
    record Advisory(String cve, String severity, TreeSet<String> packages, String installed, String fixedIn) {
    }

    /** The critical and high advisories in log4j, worst first, each with the first release after the installed one that fixes it. */
    static List<Advisory> advisories(Scan scan) {
        java.util.Map<String, Advisory> byCve = new java.util.TreeMap<>();
        for (Scan.Finding f : scan.severeIn("log4j")) {
            String fix = f.fixedIn().stream().filter(v -> Versions.compare(v, f.version()) > 0).min(Versions::compare).orElse("none");
            Advisory a = byCve.computeIfAbsent(f.cve(), k -> new Advisory(k, f.severity(), new TreeSet<>(), f.version(), fix));
            a.packages().add(f.packageName());
        }
        return byCve.values().stream().sorted(java.util.Comparator.comparing((Advisory a) -> !a.severity().equals("Critical")).thenComparing(Advisory::cve)).toList();
    }

    private static void act2Scan(StringBuilder out, Lab lab, Sbom sbom, Scan scan) throws IOException, InterruptedException {
        line(out, "TWO. Scanning it against known advisories.");
        line(out, "  THE SCAN (the input):");
        row(out, 10, "command", "grype shop-sbom:" + Lab.OLD + " -o json");
        row(out, 10, "database", "advisories, built within 7 days: " + yesNo(databaseFresh()));
        line(out, "  THE CRITICAL AND HIGH ADVISORIES IN log4j (the result):");
        List<Advisory> advisories = advisories(scan);
        for (Advisory a : advisories) {
            line(out, "    " + String.format("%-10s%-16s%s", a.severity().toUpperCase(java.util.Locale.ROOT), a.cve(), "fixed in " + a.fixedIn()));
        }
        line(out, "  HOW ONE MATCH IS MADE (" + advisories.get(0).cve() + "):");
        Advisory first = advisories.get(0);
        row(out, 15, "the bill says", String.join(", ", first.packages()) + " " + first.installed());
        row(out, 15, "the advisory", "fixed in " + first.fixedIn());
        row(out, 15, "compared", first.installed() + " < " + first.fixedIn() + ": " + yesNo(Versions.compare(first.installed(), first.fixedIn()) < 0) + ", so it matches");
        Path file = Files.writeString(lab.work().resolve("sbom.cdx.json"), sbom.json());
        Scan fromSbom = Scan.ofSbom(file);
        line(out, "  THE SAME SCAN, FROM THE BILL ALONE:");
        row(out, 10, "command", "grype sbom:sbom.cdx.json -o json");
        row(out, 10, "log4j", "the same advisories: " + yesNo(scan.ids("log4j").equals(fromSbom.ids("log4j"))));
        row(out, 10, "the rest", "in the base image's OS packages: " + yesNo(scan.findings().stream().anyMatch(f -> !f.packageName().startsWith("log4j"))));
        row(out, 10, "", "(their number changes daily, so it is not printed)");
    }

    private static void act3Upgrade(StringBuilder out, Sbom before, Scan beforeScan, Sbom after, Scan afterScan) {
        line(out, "THREE. Fixing it, and checking that it is fixed.");
        String highest = advisories(beforeScan).stream().map(Advisory::fixedIn).max(Versions::compare).orElse("none");
        line(out, "  THE UPGRADE (the input):");
        row(out, 14, "fixes needed", String.join(", ", advisories(beforeScan).stream().map(Advisory::fixedIn).sorted(Versions::compare).toList()));
        row(out, 14, "so at least", highest);
        row(out, 14, "new image", "shop-sbom:" + Lab.NEW);
        TreeSet<String> removed = new TreeSet<>(before.libraries());
        removed.removeAll(after.libraries());
        TreeSet<String> added = new TreeSet<>(after.libraries());
        added.removeAll(before.libraries());
        line(out, "  THE TWO BILLS COMPARED (the result):");
        row(out, 13, "removed", shortNames(removed));
        row(out, 13, "added", shortNames(added));
        row(out, 13, "OS packages", "the same in both: " + yesNo(before.osPackages().equals(after.osPackages())));
        line(out, "  THE NEW SCAN:");
        row(out, 28, "critical and high in log4j", "before " + beforeScan.severeIn("log4j").stream().map(Scan.Finding::cve).distinct().count()
                + ", after " + afterScan.severeIn("log4j").stream().map(Scan.Finding::cve).distinct().count());
    }

    static final String REASON = "a denial of service needing an admin's config";

    private static void act4Gate(StringBuilder out, Scan vulnerable, Scan patched, Scan current) {
        line(out, "FOUR. A gate that reads the scan.");
        line(out, "  THE RULE:");
        line(out, "    refuse a critical or high advisory in the store's libraries");
        line(out, "    a written exception may cover a high one, until its end day");
        String high = advisories(patched).stream().filter(a -> a.severity().equals("High")).map(Advisory::cve).findFirst().orElse("none");
        List<Gate.Waiver> waivers = List.of(new Gate.Waiver(high, REASON, 120));
        line(out, "  THE EXCEPTION (written down):");
        row(out, 11, "advisory", high + " (high)");
        row(out, 11, "reason", REASON);
        row(out, 11, "ends", "on day 120");
        line(out, "  SIX REQUESTS TO DEPLOY, AND THE ANSWERS:");
        String[][] requests = {
                {"shop-sbom:" + Lab.OLD, Gate.check(vulnerable, List.of(), 100)},
                {Lab.OLD + ", exception, day 100", Gate.check(vulnerable, waivers, 100)},
                {"shop-sbom:2.16.0", Gate.check(patched, List.of(), 100)},
                {"2.16.0, exception, day 100", Gate.check(patched, waivers, 100)},
                {"2.16.0, exception, day 130", Gate.check(patched, waivers, 130)},
                {"shop-sbom:" + Lab.NEW, Gate.check(current, List.of(), 130)},
        };
        for (String[] r : requests) {
            line(out, "    " + String.format("%-29s%s", r[0], r[1]));
        }
    }

    private static void act5Bill(StringBuilder out, Sbom sbom, String renamed) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        Sbom hidden = Sbom.of(renamed);
        Scan hiddenScan = Scan.ofImage(renamed);
        line(out, "  A RENAMED JAR (the input):");
        row(out, 10, "image", renamed);
        row(out, 10, "/app/lib", "blob.dat: the log4j-core-" + Lab.OLD + ".jar, renamed");
        line(out, "  WHAT THE TOOLS SEE (the result):");
        row(out, 23, "syft: a log4j library", yesNo(!hidden.withName("log4j").isEmpty()));
        row(out, 23, "grype: an advisory", yesNo(!hiddenScan.inPackages("log4j").isEmpty()) + "  <- ONLY WHAT SYFT KNOWS");
        line(out, "  A STALE BILL:");
        row(out, 14, "written for", sbom.subject());
        row(out, 14, "offered for", "shop-sbom:" + Lab.NEW);
        row(out, 14, "describes it", yesNo(sbom.subject().equals("shop-sbom:" + Lab.NEW)) + "  <- WRITE THE BILL AT EVERY BUILD");
        line(out, "  WHAT A SCAN CANNOT DO:");
        line(out, "    see tomorrow's advisories: keep bills, rescan on a schedule");
        line(out, "    know if the store calls the code: a VEX statement says");
        line(out, "    say who built the image: signed provenance (plain project)");
    }

    /** Whether grype's advisory database was built in the last seven days, so the scan is current. */
    /** Downloads grype's advisory database when it is older than seven days, so the scan and its database line never depend on its age. */
    private static void ensureFreshDatabase() throws IOException, InterruptedException {
        if (!databaseFresh()) {
            Lab.run(List.of("grype", "db", "update"));
        }
    }

    private static boolean databaseFresh() throws IOException, InterruptedException {
        Matcher m = Pattern.compile("Built:\\s+(\\S+)").matcher(Lab.run(List.of("grype", "db", "status")));
        return m.find() && Duration.between(Instant.parse(m.group(1)), Instant.now()).toDays() <= 7;
    }
}
