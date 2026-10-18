package com.jk.explore.security.asvs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Runs the story in five numbered acts: choose a level, write requirements down, check them, report coverage, and count the bill. */
public class AsvsDemo {

    static final Profile BROCHURE = new Profile("a brochure site", Profile.Sensitivity.LOW, false, false, false);
    static final Profile STORE = new Profile("the online store", Profile.Sensitivity.MEDIUM, true, false, false);
    static final Profile BANK = new Profile("the online bank", Profile.Sensitivity.HIGH, true, true, true);

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        try {
            Path dir = Files.createTempDirectory("asvs-demo");
            try {
                act1Level(out);
                List<Requirement> requirements = act2Requirements(out, dir);
                act3Checks(out);
                act4Coverage(out, requirements);
                act5Bill(out, requirements);
            } finally {
                try (var files = Files.list(dir)) {
                    for (Path p : files.toList()) {
                        Files.deleteIfExists(p);
                    }
                }
                Files.deleteIfExists(dir);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Level(StringBuilder out) {
        line(out, "ONE. Choosing a level.");
        for (Profile p : List.of(BROCHURE, STORE, BANK)) {
            LevelChooser.Choice choice = LevelChooser.choose(p);
            line(out, "  " + p.name() + ": " + choice.level());
            for (String reason : choice.reasons()) {
                line(out, "    - " + reason);
            }
        }
        List<Requirement> all = Catalogue.all();
        line(out, "  of " + all.size() + " requirements, L1 applies " + applicable(all, Level.L1) + ", L2 applies " + applicable(all, Level.L2)
                + " and L3 applies " + applicable(all, Level.L3) + ": each level includes the ones below it");
    }

    private static long applicable(List<Requirement> all, Level level) {
        return all.stream().filter(r -> level.covers(r.level())).count();
    }

    private static List<Requirement> act2Requirements(StringBuilder out, Path dir) throws IOException {
        line(out, "TWO. Requirements written down as data.");
        Path file = dir.resolve("security-requirements.tsv");
        RequirementsFile.write(file, Catalogue.all());
        List<Requirement> back = RequirementsFile.read(file);
        line(out, "  security-requirements.tsv holds " + back.size() + " requirements, and reads back the same: " + back.equals(Catalogue.all()));
        Requirement first = back.get(0);
        line(out, "  for example " + first.id() + " (" + first.chapter() + ", " + first.level() + ", " + first.verification().name().toLowerCase(Locale.ROOT)
                + "): " + first.statement());
        Map<Verification, Long> how = back.stream().collect(Collectors.groupingBy(Requirement::verification, () -> new EnumMap<>(Verification.class), Collectors.counting()));
        line(out, "  verified by an automated test: " + how.get(Verification.AUTOMATED) + ", by a configuration check: " + how.get(Verification.CONFIGURATION)
                + ", by a person: " + how.get(Verification.MANUAL));
        line(out, "  the statements are the team's own words and the ids are the team's own; each one names its chapter of ASVS");
        return back;
    }

    private static void act3Checks(StringBuilder out) {
        line(out, "THREE. Checks that ask the running store.");
        SampleApp store = new SampleApp(SampleApp.Settings.store());
        int passed = 0;
        for (Checks.Check check : Checks.all().values()) {
            boolean ok = check.passes().test(store);
            passed += ok ? 1 : 0;
            line(out, "  " + check.question() + ": " + (ok ? "pass" : "FAIL"));
        }
        line(out, "  " + passed + " of " + Checks.all().size() + " checks pass on the store as it is configured today");
        SampleApp fast = new SampleApp(SampleApp.Settings.store().withFastPasswordHash());
        line(out, "  the same password check on a build that stores passwords as plain SHA-256: "
                + (Checks.named("password-storage").passes().test(fast) ? "pass" : "FAIL") + "  <- A CHECK CATCHES WHAT A REVIEW MIGHT MISS");
    }

    private static void act4Coverage(StringBuilder out, List<Requirement> requirements) {
        line(out, "FOUR. Coverage and gaps.");
        Level level = LevelChooser.choose(STORE).level();
        Assessment today = Assessment.of(requirements, level, new SampleApp(SampleApp.Settings.store()));
        Map<Assessment.Status, Integer> t = today.totals();
        line(out, "  the store at " + level + ": met " + t.get(Assessment.Status.MET) + ", failed " + t.get(Assessment.Status.FAILED)
                + ", still manual " + t.get(Assessment.Status.MANUAL) + ", not applicable " + t.get(Assessment.Status.NOT_APPLICABLE));
        for (Map.Entry<String, Map<Assessment.Status, Integer>> chapter : today.byChapter().entrySet()) {
            Map<Assessment.Status, Integer> c = chapter.getValue();
            line(out, "    " + chapter.getKey() + ": met " + c.getOrDefault(Assessment.Status.MET, 0) + ", failed "
                    + c.getOrDefault(Assessment.Status.FAILED, 0) + ", manual " + c.getOrDefault(Assessment.Status.MANUAL, 0));
        }
        line(out, "  the gap: " + String.join(", ", today.failing()) + ", staff sign in without a second factor");
        Assessment regressed = Assessment.of(requirements, level, new SampleApp(SampleApp.Settings.store().withDebug(true)));
        List<String> added = regressed.newlyFailingSince(today);
        line(out, "  a release that ships with the debug flag on: newly failing " + String.join(", ", added)
                + "  <- THE RELEASE GATE REFUSES IT");
        Assessment fixed = Assessment.of(requirements, level, new SampleApp(SampleApp.Settings.store().withStaffMfa(true)));
        line(out, "  once staff sign in with a second factor: failed " + fixed.totals().get(Assessment.Status.FAILED)
                + ", and the report says what is left for people to check: " + fixed.totals().get(Assessment.Status.MANUAL));
    }

    private static void act5Bill(StringBuilder out, List<Requirement> requirements) {
        line(out, "FIVE. The bill.");
        Level bank = LevelChooser.choose(BANK).level();
        Assessment atBank = Assessment.of(requirements, bank, new SampleApp(SampleApp.Settings.store().withStaffMfa(true)));
        line(out, "  the same store judged at the bank's " + bank + ": " + atBank.totals().get(Assessment.Status.MANUAL)
                + " requirements need a person: " + manualIds(atBank) + ", and no test replaces that review");
        line(out, "  a standard says what to verify, not which threats this system faces: the threat model comes first, and its mitigations"
                + " become extra requirements");
        List<String> old = requirements.stream().filter(r -> !r.edition().equals(Catalogue.EDITION)).map(r -> r.id() + " (" + r.edition() + ")").toList();
        line(out, "  requirements mapped against an older edition of the standard: " + String.join(", ", old) + "  <- REVIEW THE MAPPING");
        line(out, "  a report where every check passes means these checks pass, not that the store is secure");
    }

    private static String manualIds(Assessment a) {
        return a.results().stream().filter(r -> r.status() == Assessment.Status.MANUAL).map(r -> r.requirement().id()).collect(Collectors.joining(", "));
    }
}
