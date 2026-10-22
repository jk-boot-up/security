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
        Scan scan = Scan.ofImage(old);
        act1WhatItAnswers(out, sbom);
        act2Scan(out, lab, old, sbom, scan);
        Sbom newer = Sbom.of(current);
        Scan newerScan = Scan.ofImage(current);
        act3Upgrade(out, sbom, scan, newer, newerScan);
        act4Gate(out, scan, Scan.ofImage(patched), newerScan);
        act5Bill(out, lab, sbom, current, renamed);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1WhatItAnswers(StringBuilder out, Sbom sbom) {
        line(out, "ONE. What a bill of materials answers, and what it does not.");
        line(out, "  syft catalogues the image shop-sbom:" + Lab.OLD + " and writes a CycloneDX JSON bill of materials: " + sbom.subject().equals("shop-sbom:" + Lab.OLD) + ", with operating system packages: "
                + sbom.libraries().stream().anyMatch(p -> p.startsWith("pkg:apk/")) + " and libraries: " + sbom.libraries().stream().anyMatch(p -> p.startsWith("pkg:maven/")));
        line(out, "  the application's libraries, each by its package URL: " + sbom.withName("log4j"));
        line(out, "  each library jar carries its SHA-1: " + sbom.mentions("\"SHA-1\"") + ", and its licence: " + sbom.mentions("Apache-2.0"));
        line(out, "  the bill of materials names the image, and not the repository, the commit or the builder: " + !sbom.document().containsKey("vcs") + "  <- THAT IS WHAT PROVENANCE IS FOR");
    }

    private static List<Scan.Finding> severe(Scan scan) {
        return scan.inPackages("log4j").stream().filter(Scan.Finding::severe).toList();
    }

    private static TreeSet<String> aliases(List<Scan.Finding> findings, String severity) {
        TreeSet<String> out = new TreeSet<>();
        findings.stream().filter(f -> f.severity().equals(severity)).forEach(f -> out.add(f.alias().isEmpty() ? f.id() : f.alias()));
        return out;
    }

    private static void act2Scan(StringBuilder out, Lab lab, String image, Sbom sbom, Scan scan) throws IOException, InterruptedException {
        line(out, "TWO. Scanning it against known advisories.");
        List<Scan.Finding> severe = severe(scan);
        line(out, "  grype reads the image and matches its packages to its vulnerability database: critical advisories in log4j: " + aliases(severe, "Critical") + ", high: " + aliases(severe, "High"));
        line(out, "  each says which version fixes it: " + fixes(severe));
        Path file = Files.writeString(lab.work().resolve("sbom.cdx.json"), sbom.json());
        Scan fromSbom = Scan.ofSbom(file);
        line(out, "  scanning the bill of materials alone, not the image, finds the same advisories in log4j: " + scan.ids("log4j").equals(fromSbom.ids("log4j")));
        line(out, "  the rest of the findings come from the base image's operating system packages: " + scan.findings().stream().anyMatch(f -> !f.packageName().startsWith("log4j")) + "  (their number changes every day, so it is not printed)");
        line(out, "  the image was never run: the bill of materials is read from its layers");
    }

    private static String fixes(List<Scan.Finding> severe) {
        TreeSet<String> fixes = new TreeSet<>(SbomScanDemo::compareVersions);
        severe.forEach(f -> fixes.addAll(f.fixedIn()));
        return fixes.toString();
    }

    private static int compareVersions(String a, String b) {
        List<Integer> x = version(a);
        List<Integer> y = version(b);
        for (int i = 0; i < Math.max(x.size(), y.size()); i++) {
            int c = Integer.compare(i < x.size() ? x.get(i) : 0, i < y.size() ? y.get(i) : 0);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /** A version as numbers, so that 2.9 sorts before 2.17. */
    private static List<Integer> version(String v) {
        List<Integer> parts = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+").matcher(v);
        while (m.find()) {
            parts.add(Integer.parseInt(m.group()));
        }
        return parts;
    }

    private static void act3Upgrade(StringBuilder out, Sbom before, Scan beforeScan, Sbom after, Scan afterScan) {
        line(out, "THREE. Fixing it, and checking that it is fixed.");
        TreeSet<String> removed = new TreeSet<>(before.libraries());
        removed.removeAll(after.libraries());
        TreeSet<String> added = new TreeSet<>(after.libraries());
        added.removeAll(before.libraries());
        line(out, "  the upgrade that clears every critical and high advisory in log4j is " + fixes(severe(beforeScan)).replaceAll("^\\[.*, ([^,]+)]$", "$1") + " or later; the team builds shop-sbom:" + Lab.NEW);
        line(out, "  the new bill of materials differs in exactly the libraries that changed: removed " + removed + ", added " + added);
        line(out, "  the base image's packages are the same in both: " + (before.libraries().stream().filter(p -> p.startsWith("pkg:apk/")).toList().equals(after.libraries().stream().filter(p -> p.startsWith("pkg:apk/")).toList())));
        line(out, "  the scan of the new image: critical and high advisories in log4j: " + severe(afterScan).size() + ", where the old image had " + severe(beforeScan).size());
    }

    /** The deployment gate: no critical or high advisory in the store's own libraries, except one that has a written exception that has not run out. */
    record Waiver(String id, String reason, int expiresOnDay) {
    }

    static String gate(Scan scan, List<Waiver> exceptions, int today) {
        List<Scan.Finding> blocking = new ArrayList<>();
        int excepted = 0;
        for (Scan.Finding f : severe(scan)) {
            boolean covered = f.severity().equals("High") && exceptions.stream().anyMatch(e -> (e.id().equals(f.id()) || e.id().equals(f.alias())) && e.expiresOnDay() >= today);
            if (covered) {
                excepted++;
            } else {
                blocking.add(f);
            }
        }
        return blocking.isEmpty() ? "deployed" + (excepted > 0 ? " with exceptions for its high advisories" : "") : "refused (" + aliases(blocking, "Critical").size() + " critical and " + aliases(blocking, "High").size() + " high advisories)";
    }

    private static void act4Gate(StringBuilder out, Scan vulnerable, Scan patched, Scan current) {
        line(out, "FOUR. A gate that reads the scan.");
        line(out, "  the gate refuses a critical or high advisory in the store's own libraries; shop-sbom:" + Lab.OLD + ": " + gate(vulnerable, List.of(), 100));
        List<Waiver> exceptions = severe(patched).stream().filter(f -> f.severity().equals("High")).map(f -> new Waiver(f.id(), "a denial of service that needs an administrator's configuration file; fixed in the next release", 120)).toList();
        List<Waiver> highOnly = List.of(new Waiver("CVE-2021-45105", "a denial of service that needs an administrator's configuration file; fixed in the next release", 120));
        line(out, "  an exception for a high advisory cannot cover a critical one: the same image with the exception written down: " + gate(vulnerable, highOnly, 100));
        line(out, "  shop-sbom:2.16.0, which fixes the critical ones: " + gate(patched, List.of(), 100));
        line(out, "  with the written exception, on day 100: " + gate(patched, exceptions, 100) + "; on day 130, after it ran out on day 120: " + gate(patched, exceptions, 130));
        line(out, "  shop-sbom:" + Lab.NEW + ": " + gate(current, List.of(), 130));
        line(out, "  an exception names the advisory, the reason and the day it ends, so that it is reviewed and cannot stay for ever");
    }

    private static void act5Bill(StringBuilder out, Lab lab, Sbom sbom, String current, String renamed) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        Sbom hidden = Sbom.of(renamed);
        Scan hiddenScan = Scan.ofImage(renamed);
        line(out, "  the same log4j-core " + Lab.OLD + " jar, renamed blob.dat inside another image: the bill of materials lists a log4j library: " + !hidden.withName("log4j").isEmpty()
                + ", the scan finds a log4j advisory: " + !hiddenScan.inPackages("log4j").isEmpty() + "  <- A SCANNER FINDS ONLY WHAT THE CATALOGUER RECOGNISES");
        line(out, "  the bill of materials written for shop-sbom:" + Lab.OLD + ", offered for shop-sbom:" + Lab.NEW + ": describes it " + sbom.subject().equals("shop-sbom:" + Lab.NEW) + "  <- WRITE THE BILL AT EVERY BUILD");
        line(out, "  the database that the scan matches against is data: it was built " + databaseAgeDays() + " days ago or less");
        line(out, "  an advisory published tomorrow will match a bill of materials written today, so the bills must be kept and scanned again on a schedule");
        line(out, "  a finding is a question, not a verdict: a vulnerable library the store never calls is still reported; a statement of exploitability (VEX) records the answer");
        line(out, "  the signed provenance of how the image was built, which this bill does not hold, is in the plain sbom-and-provenance project and in the cosign project");
    }

    private static String databaseAgeDays() throws IOException, InterruptedException {
        Matcher m = Pattern.compile("Built:\\s+(\\S+)").matcher(Lab.run(List.of("grype", "db", "status")));
        long days = m.find() ? Duration.between(Instant.parse(m.group(1)), Instant.now()).toDays() : 999;
        return days <= 7 ? "7" : days <= 14 ? "14" : "more than 14";
    }
}
