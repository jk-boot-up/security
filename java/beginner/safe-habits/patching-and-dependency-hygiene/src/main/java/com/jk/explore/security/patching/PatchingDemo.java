package com.jk.explore.security.patching;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class PatchingDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Everything is fixed: nothing here is random or reads the clock. */
    public static String run() throws IOException {
        Sbom sbom = Sbom.parse(resource("sbom.json"));
        List<Advisory> advisories = Advisory.parse(resource("advisories.json"));
        List<Finding> findings = Audit.scan(sbom, advisories);
        StringBuilder out = new StringBuilder();
        act1Inventory(out, sbom);
        act2Matching(out, advisories, findings);
        act3ScoresAndTriage(out, findings);
        act4PatchWindows(out, findings);
        act5Pinning(out);
        return out.toString();
    }

    static String resource(String name) throws IOException {
        try (InputStream in = PatchingDemo.class.getResourceAsStream("/" + name)) {
            if (in == null) {
                throw new IOException("missing resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Inventory(StringBuilder out, Sbom sbom) {
        line(out, "ONE. An inventory of what the store ships.");
        int libraries = sbom.libraries().size();
        int direct = sbom.direct().size();
        line(out, "  the store's software lists what it is built from, in a small JSON file: " + (libraries + 1) + " components, the store itself and " + libraries + " libraries");
        line(out, "  " + direct + " libraries are direct choices and " + (libraries - direct) + " arrive on their own as dependencies of dependencies");
        for (String entry : sbom.tree()) {
            line(out, "    " + entry);
        }
        line(out, "  nobody on the team chose acme-json, acme-crypto or acme-xml: they came with something else");
    }

    private static void act2Matching(StringBuilder out, List<Advisory> advisories, List<Finding> findings) {
        line(out, "TWO. Matching the inventory against known problems.");
        line(out, "  the advisory file lists " + advisories.size() + " known problems (fictional, made up for this lesson)");
        line(out, "  as text, 1.10.0 sorts before 1.9.0: " + ("1.10.0".compareTo("1.9.0") < 0)
                + "; as versions, 1.10.0 is newer: " + (SemVer.parse("1.10.0").compareTo(SemVer.parse("1.9.0")) > 0));
        VersionRange range = VersionRange.parse(">=1.5.0 <1.8.3");
        line(out, "  acme-json 1.8.2 is in the affected range " + range + ": " + range.contains(SemVer.parse("1.8.2"))
                + "; 1.8.3, the fixed version, is: " + range.contains(SemVer.parse("1.8.3")));
        line(out, "  problems that apply to this store: " + findings.size() + " of " + advisories.size());
        for (Finding finding : findings) {
            line(out, "  " + finding.advisory().id() + " in " + finding.componentRef() + ", reached through " + finding.chain());
            line(out, "    " + finding.advice());
        }
        line(out, "  the other " + (advisories.size() - findings.size()) + " do not apply: the installed acme-crypto and acme-http are already the fixed versions");
    }

    private static void act3ScoresAndTriage(StringBuilder out, List<Finding> findings) {
        line(out, "THREE. How bad, and what to fix first.");
        String[][] published = {
            {"AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", "9.8"}, {"AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H", "10.0"},
            {"AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:H", "7.8"}, {"AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N", "5.3"},
            {"AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N", "6.1"}, {"AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "7.5"},
        };
        boolean allMatch = true;
        StringBuilder scores = new StringBuilder();
        for (String[] example : published) {
            allMatch &= String.format(Locale.ROOT, "%.1f", Cvss31.base(example[0])).equals(example[1]);
            scores.append(scores.length() == 0 ? "" : ", ").append(example[1]);
        }
        line(out, "  the score comes from the CVSS 3.1 equation; it reproduces six published scores (" + scores + "): " + allMatch);
        for (Finding finding : findings) {
            line(out, "  " + finding.advisory().id() + ": " + String.format(Locale.ROOT, "%.1f", finding.score()) + " " + finding.severity()
                    + ", the store " + (finding.reachable() ? "uses" : "does not use") + " the feature " + finding.advisory().feature()
                    + ": " + (finding.reachable() ? "reachable" : "not reachable"));
        }
        int rank = 1;
        for (Finding finding : Audit.triage(findings)) {
            line(out, "  " + rank++ + ". " + finding.advisory().id() + ": " + finding.priority());
        }
        line(out, "  a high score that cannot be reached is still a debt: schedule it, do not ignore it");
    }

    private static void act4PatchWindows(StringBuilder out, List<Finding> findings) {
        line(out, "FOUR. Patch windows.");
        line(out, "  time is counted in days on a simulated clock; the deadlines are Critical 7, High 30, Medium 90, Low 180");
        int today = 40;
        int[] publishedDay = {0, 0, 12};
        Integer[] patchedDay = {5, null, null};
        line(out, "  today is day " + today);
        int met = 0;
        for (int i = 0; i < findings.size(); i++) {
            Finding finding = findings.get(i);
            PatchWindow.Status status = PatchWindow.evaluate(finding.severity(), publishedDay[i], patchedDay[i], today);
            if (status.withinWindow()) {
                met++;
            }
            line(out, "  " + finding.advisory().id() + " (" + finding.severity() + "): published on day " + publishedDay[i] + ", " + status.text());
        }
        line(out, "  inside the window: " + met + " of " + findings.size());
        line(out, "  the breach is the finding judged not reachable: low-priority work quietly ages unless it has a deadline too");
    }

    private static void act5Pinning(StringBuilder out) {
        line(out, "FIVE. Pinning, and checking what you download.");
        byte[] json = "acme-json 1.8.3 jar contents".getBytes(StandardCharsets.UTF_8);
        byte[] xml = "acme-xml 3.1.5 jar contents".getBytes(StandardCharsets.UTF_8);
        byte[] log = "acme-log 4.2.1 jar contents".getBytes(StandardCharsets.UTF_8);
        Pins pins = new Pins();
        pins.pin("acme-json@1.8.3", json);
        pins.pin("acme-xml@3.1.5", xml);
        pins.pin("acme-log@4.2.1", log);
        line(out, "  the SHA-256 of each artifact is recorded when a person reviews it: " + pins.size() + " pinned");
        line(out, "  acme-json@1.8.3 downloaded again: " + pins.verify("acme-json@1.8.3", json));
        byte[] tampered = json.clone();
        tampered[0] ^= 1;
        line(out, "  one bit changed, same name and version: " + pins.verify("acme-json@1.8.3", tampered) + "  <- REFUSED");
        line(out, "  a version nobody has reviewed, acme-json@1.8.4: " + pins.verify("acme-json@1.8.4", json) + "  <- REFUSED UNTIL SOMEONE REVIEWS IT");
        List<String> known = List.of("acme-json", "acme-xml", "acme-log", "acme-http", "acme-image", "acme-crypto");
        line(out, "  a name one letter away from a real one, acme-jsom, looks like a copy: " + Typosquat.looksLikeACopy("acme-jsom", known));
        line(out, "  acme-json itself looks like a copy: " + Typosquat.looksLikeACopy("acme-json", known));
        line(out, "  Gradle can enforce hashes for every dependency from a file, gradle/verification-metadata.xml (described here, not run)");
        line(out, "  the cost: every update needs a review of the new hash, so automate the pull requests and keep the review small");
    }
}
