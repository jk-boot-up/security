package com.jk.explore.security.patching;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
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

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    private static String name(String ref) {
        return ref.substring(0, ref.indexOf('@'));
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String w(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void act1Inventory(StringBuilder out, Sbom sbom) {
        line(out, "ONE. An inventory of what the store ships.");
        int libraries = sbom.libraries().size();
        line(out, "  THE INVENTORY (sbom.json: " + (libraries + 1) + " components, " + libraries + " libraries):");
        for (String entry : sbom.tree()) {
            line(out, "    " + entry);
        }
        List<String> direct = sbom.direct().stream().map(PatchingDemo::name).toList();
        List<String> transitive = sbom.components().stream().map(Component::ref)
                .filter(ref -> sbom.libraries().contains(ref) && !sbom.direct().contains(ref))
                .map(PatchingDemo::name).toList();
        line(out, "  WHO CHOSE THEM:");
        line(out, "    direct, chosen by the team:  " + String.join(" ", direct));
        line(out, "    transitive, came with them:  " + String.join(" ", transitive));
        line(out, "  nobody chose " + transitive.size() + " of the " + libraries + ": they came with something else");
    }

    private static void act2Matching(StringBuilder out, List<Advisory> advisories, List<Finding> findings) {
        line(out, "TWO. Matching the inventory against known problems.");
        line(out, "  THE ADVISORIES (" + advisories.size() + ", fictional, made up for this lesson):");
        for (Advisory advisory : advisories) {
            line(out, "    " + advisory.id() + "  " + pad(advisory.component(), 12) + pad(advisory.affected().toString(), 16)
                    + "fixed " + advisory.fixed());
        }
        line(out, "  COMPARING VERSIONS:");
        line(out, "    as text, 1.10.0 comes before 1.9.0: " + ("1.10.0".compareTo("1.9.0") < 0) + "  <- WRONG");
        line(out, "    as numbers, 1.10.0 is newer (10 > 9): "
                + (SemVer.parse("1.10.0").compareTo(SemVer.parse("1.9.0")) > 0));
        VersionRange range = VersionRange.parse(">=1.5.0 <1.8.3");
        line(out, "    1.8.2 is in " + range + ": " + range.contains(SemVer.parse("1.8.2"))
                + ", 1.8.3 is: " + range.contains(SemVer.parse("1.8.3")));
        line(out, "  THE MATCHES (" + findings.size() + " of " + advisories.size() + " apply to this store):");
        for (Finding finding : findings) {
            line(out, "    " + finding.advisory().id() + "  " + finding.componentRef());
            line(out, "      via " + String.join(" > ", finding.path().stream().map(PatchingDemo::name).toList()));
            line(out, "      fix: " + (finding.direct() ? "change " : "force ") + finding.advisory().component() + " to "
                    + finding.advisory().fixed() + (finding.direct() ? " (direct)" : " (transitive)"));
        }
        line(out, "  the other " + (advisories.size() - findings.size()) + " are already at their fixed versions");
    }

    private static void act3ScoresAndTriage(StringBuilder out, List<Finding> findings) {
        line(out, "THREE. How bad, and what to fix first.");
        String[][] published = {
            {"AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", "9.8"}, {"AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H", "10.0"},
            {"AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:H", "7.8"}, {"AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N", "5.3"},
            {"AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N", "6.1"}, {"AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "7.5"},
        };
        boolean allMatch = true;
        for (String[] example : published) {
            allMatch &= String.format(Locale.ROOT, "%.1f", Cvss31.base(example[0])).equals(example[1]);
        }
        Finding top = findings.get(0);
        Cvss31.Steps s = Cvss31.steps(top.advisory().vector());
        line(out, "  THE VECTOR (" + top.advisory().id() + "):");
        line(out, "    " + top.advisory().vector());
        line(out, "  THE SCORE, STEP BY STEP (CVSS 3.1, scope unchanged):");
        line(out, "    ISS     1 - (1-" + w(s.confidentiality()) + ")(1-" + w(s.integrity()) + ")(1-"
                + w(s.availability()) + ") = " + num(s.iss()));
        line(out, "    impact  6.42 x " + num(s.iss()) + " = " + num(s.impact()));
        line(out, "    exploit 8.22 x " + w(s.attack()) + " x " + w(s.complexity()) + " x " + w(s.privileges()) + " x "
                + w(s.interaction()) + " = " + num(s.exploitability()));
        line(out, "    sum     " + num(s.impact()) + " + " + num(s.exploitability()) + " = " + num(s.sum()));
        line(out, "    score   " + num(s.sum()) + " rounded up = " + s.score() + " " + Cvss31.severity(s.score()));
        line(out, "  six published scores reproduced: " + allMatch);
        line(out, "  THE FINDINGS (score, feature, used by the store?):");
        for (Finding finding : findings) {
            line(out, "    " + finding.advisory().id() + "  " + pad(String.format(Locale.ROOT, "%.1f", finding.score()) + " "
                    + finding.severity(), 14) + pad(finding.advisory().feature(), 17)
                    + (finding.reachable() ? "yes" : "no"));
        }
        line(out, "  THE ORDER TO FIX THEM:");
        int rank = 1;
        for (Finding finding : Audit.triage(findings)) {
            line(out, "    " + rank++ + ". " + finding.advisory().id() + "  " + finding.priority());
        }
        line(out, "  a high score nobody can reach is still a debt: schedule it");
    }

    private static void act4PatchWindows(StringBuilder out, List<Finding> findings) {
        line(out, "FOUR. Patch windows.");
        line(out, "  THE DEADLINES (days to patch, by severity):");
        line(out, "    Critical " + PatchWindow.slaDays("Critical") + ", High " + PatchWindow.slaDays("High")
                + ", Medium " + PatchWindow.slaDays("Medium") + ", Low " + PatchWindow.slaDays("Low"));
        int today = 40;
        int[] publishedDay = {0, 0, 12};
        Integer[] patchedDay = {5, null, null};
        line(out, "  THE FINDINGS ON DAY " + today + " (a simulated clock):");
        line(out, "    id              published deadline state");
        int met = 0;
        for (int i = 0; i < findings.size(); i++) {
            Finding finding = findings.get(i);
            int deadline = publishedDay[i] + PatchWindow.slaDays(finding.severity());
            PatchWindow.Status status = PatchWindow.evaluate(finding.severity(), publishedDay[i], patchedDay[i], today);
            if (status.withinWindow()) {
                met++;
            }
            String state = patchedDay[i] != null
                    ? "patched day " + patchedDay[i] + (status.withinWindow() ? ", in time" : ", LATE")
                    : status.withinWindow() ? "open, " + (deadline - today) + " days left"
                    : "BREACHED " + (today - deadline) + " days ago";
            line(out, "    " + finding.advisory().id() + "  " + pad("day " + publishedDay[i], 10) + pad("day " + deadline, 9)
                    + state);
        }
        line(out, "  inside the window: " + met + " of " + findings.size());
        line(out, "  the breach is the one judged not reachable: low-priority");
        line(out, "  work ages quietly unless it has a deadline too");
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
        line(out, "  THE PINS (SHA-256, recorded when a person reviews it):");
        for (String ref : List.of("acme-json@1.8.3", "acme-xml@3.1.5", "acme-log@4.2.1")) {
            line(out, "    " + pad(ref, 17) + pins.hashOf(ref).substring(0, 32) + "...");
        }
        byte[] tampered = json.clone();
        tampered[0] ^= 1;
        line(out, "  THE DOWNLOADS, AND THE CHECK:");
        line(out, "    acme-json@1.8.3  as reviewed    " + pins.verify("acme-json@1.8.3", json));
        line(out, "    acme-json@1.8.3  1 bit flipped  " + pins.verify("acme-json@1.8.3", tampered));
        line(out, "    acme-json@1.8.4  not reviewed   " + pins.verify("acme-json@1.8.4", json));
        line(out, "  only VERIFIED is used  <- MISMATCH AND NOT_PINNED ARE REFUSED");
        line(out, "  ONE BIT FLIPPED (first byte 0x" + HexFormat.of().toHexDigits(json[0]) + " '" + (char) json[0]
                + "' becomes 0x" + HexFormat.of().toHexDigits(tampered[0]) + " '" + (char) tampered[0] + "'):");
        line(out, "    reviewed  " + Pins.sha256(json).substring(0, 32) + "...");
        line(out, "    download  " + Pins.sha256(tampered).substring(0, 32) + "...");
        List<String> known = List.of("acme-json", "acme-xml", "acme-log", "acme-http", "acme-image", "acme-crypto");
        line(out, "  NAMES ONE LETTER AWAY (a typosquat):");
        line(out, "    acme-jsom  a copy? " + Typosquat.looksLikeACopy("acme-jsom", known)
                + "   1 letter from acme-json");
        line(out, "    acme-json  a copy? " + Typosquat.looksLikeACopy("acme-json", known)
                + "  it is the real one");
        line(out, "  Gradle can enforce pins: gradle/verification-metadata.xml");
        line(out, "  the cost: a review for each new hash; keep updates small");
    }
}
