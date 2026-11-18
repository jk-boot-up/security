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

    private static void line(StringBuilder out, String format, Object... args) {
        out.append(args.length == 0 ? format : String.format(format, args)).append('\n');
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    private static String ids(List<Requirement> rs) {
        return rs.stream().map(Requirement::id).collect(Collectors.joining(" "));
    }

    private static void act1Level(StringBuilder out) {
        line(out, "ONE. Choosing a level.");
        line(out, "  THE INPUT: THREE APPLICATIONS");
        line(out, "    application       data    accounts  money  regulated");
        for (Profile p : List.of(BROCHURE, STORE, BANK)) {
            line(out, "    %-16s  %-6s  %-8s  %-5s  %s", p.name(), p.data(), yes(p.holdsAccounts()), yes(p.movesMoney()), yes(p.regulated()));
        }
        line(out, "  THE RESULT: THE LEVEL, AND WHY");
        for (Profile p : List.of(BROCHURE, STORE, BANK)) {
            LevelChooser.Choice choice = LevelChooser.choose(p);
            line(out, "    %s: %s", p.name(), choice.level());
            for (int i = 0; i < choice.reasons().size(); i++) {
                line(out, "      %s  %s", Level.values()[i], choice.reasons().get(i));
            }
        }
        List<Requirement> all = Catalogue.all();
        line(out, "  WHAT EACH LEVEL ASKS FOR, OF %d REQUIREMENTS:", all.size());
        Level below = null;
        for (Level level : Level.values()) {
            Level previous = below;
            List<Requirement> applies = all.stream().filter(r -> level.covers(r.level())).toList();
            List<Requirement> added = applies.stream().filter(r -> previous == null || !previous.covers(r.level())).toList();
            line(out, "    %s  %2d  %s%s", level, applies.size(), previous == null ? "" : previous + " + ", ids(added));
            below = level;
        }
    }

    private static List<Requirement> act2Requirements(StringBuilder out, Path dir) throws IOException {
        line(out, "TWO. Requirements written down as data.");
        line(out, "  THE INPUT: THE TEAM'S %d REQUIREMENTS", Catalogue.all().size());
        for (Requirement r : Catalogue.all()) {
            line(out, "    %s  %s  %-13s  %s", r.id(), r.level(), r.verification().name().toLowerCase(Locale.ROOT), r.chapter());
        }
        Path file = dir.resolve("security-requirements.tsv");
        RequirementsFile.write(file, Catalogue.all());
        List<Requirement> back = RequirementsFile.read(file);
        line(out, "  THE RESULT: %s", file.getFileName());
        line(out, "    lines        %d: a header and %d rows", Files.readAllLines(file).size(), back.size());
        line(out, "    read back    the same %d requirements: %s", back.size(), back.equals(Catalogue.all()));
        line(out, "  ONE ROW, FIELD BY FIELD:");
        String[] names = RequirementsFile.HEADER.split("\t");
        String[] values = Files.readAllLines(file).get(1).split("\t", -1);
        for (int i = 0; i < names.length; i++) {
            line(out, "    %-12s  %s", names[i], values[i]);
        }
        Map<Verification, Long> how = back.stream().collect(Collectors.groupingBy(Requirement::verification, () -> new EnumMap<>(Verification.class), Collectors.counting()));
        line(out, "  HOW THEY ARE VERIFIED:");
        for (Verification v : Verification.values()) {
            line(out, "    %-13s  %2d  %s", v.name().toLowerCase(Locale.ROOT), how.get(v),
                    ids(back.stream().filter(r -> r.verification() == v).toList()));
        }
        return back;
    }

    private static void act3Checks(StringBuilder out) {
        line(out, "THREE. Checks that ask the running store.");
        line(out, "  THE QUESTIONS, ONE PER CHECK:");
        Checks.all().values().forEach(c -> line(out, "    %-16s  %s", c.name(), c.question()));
        SampleApp store = new SampleApp(SampleApp.Settings.store());
        line(out, "  THE ANSWERS FROM THE STORE AS CONFIGURED TODAY:");
        int passed = 0;
        for (Checks.Check check : Checks.all().values()) {
            boolean ok = check.passes().test(store);
            passed += ok ? 1 : 0;
            line(out, "    %-16s  %s", check.name(), ok ? "pass" : "FAIL");
        }
        line(out, "    %d of %d pass", passed, Checks.all().size());
        line(out, "  HOW THE PASSWORD CHECK DECIDES:");
        line(out, "    the stored value  scheme$rounds$salt$hash");
        line(out, "    it needs          pbkdf2-sha256, rounds >= %,d", Checks.PBKDF2_MINIMUM_ROUNDS);
        line(out, "  BEFORE: THE STORE TODAY");
        String[] today = store.storedPasswordFor(Checks.SAMPLE_PASSWORD).split("\\$");
        line(out, "    stored   %s$%s$<16-byte salt>$<32-byte hash>", today[0], today[1]);
        line(out, "    check    %s, %,d >= %,d rounds: %s", today[0], Integer.parseInt(today[1]), Checks.PBKDF2_MINIMUM_ROUNDS,
                Checks.named("password-storage").passes().test(store) ? "pass" : "FAIL");
        SampleApp fast = new SampleApp(SampleApp.Settings.store().withFastPasswordHash());
        String[] plain = fast.storedPasswordFor(Checks.SAMPLE_PASSWORD).split("\\$");
        line(out, "  AFTER: A BUILD THAT USES PLAIN SHA-256");
        line(out, "    stored   %s$%s...", plain[0], plain[1].substring(0, 24));
        line(out, "    check    %s, no rounds, no salt: %s", plain[0], Checks.named("password-storage").passes().test(fast) ? "pass" : "FAIL");
        line(out, "    the same password gives the same %d hex digits on every store", plain[1].length());
    }

    private static List<Requirement> with(Assessment a, Assessment.Status status) {
        return a.results().stream().filter(r -> r.status() == status).map(Assessment.Result::requirement).toList();
    }

    private static void act4Coverage(StringBuilder out, List<Requirement> requirements) {
        line(out, "FOUR. Coverage and gaps.");
        Level level = LevelChooser.choose(STORE).level();
        Assessment today = Assessment.of(requirements, level, new SampleApp(SampleApp.Settings.store()));
        line(out, "  THE RESULT: THE STORE AT %s, REQUIREMENT BY REQUIREMENT", level);
        for (Assessment.Status s : Assessment.Status.values()) {
            List<Requirement> rs = with(today, s);
            line(out, "    %-14s %2d  %s", s.name().toLowerCase(Locale.ROOT).replace('_', ' '), rs.size(), ids(rs));
        }
        line(out, "  BY CHAPTER:");
        line(out, "    chapter                              met  failed  manual");
        for (Map.Entry<String, Map<Assessment.Status, Integer>> chapter : today.byChapter().entrySet()) {
            Map<Assessment.Status, Integer> c = chapter.getValue();
            line(out, "    %-35s  %3d  %6d  %6d", chapter.getKey(), c.getOrDefault(Assessment.Status.MET, 0),
                    c.getOrDefault(Assessment.Status.FAILED, 0), c.getOrDefault(Assessment.Status.MANUAL, 0));
        }
        line(out, "  THE GAP:");
        for (Requirement r : with(today, Assessment.Status.FAILED)) {
            line(out, "    %s  %s", r.id(), r.statement());
        }
        Assessment regressed = Assessment.of(requirements, level, new SampleApp(SampleApp.Settings.store().withDebug(true)));
        line(out, "  BEFORE THE RELEASE: failing %s", String.join(" ", today.failing()));
        line(out, "  AFTER A RELEASE WITH THE DEBUG FLAG ON:");
        line(out, "    failing         %s", String.join(" ", regressed.failing()));
        line(out, "    newly failing   %s  <- THE RELEASE GATE REFUSES IT", String.join(" ", regressed.newlyFailingSince(today)));
        Assessment fixed = Assessment.of(requirements, level, new SampleApp(SampleApp.Settings.store().withStaffMfa(true)));
        line(out, "  AFTER STAFF SIGN IN WITH A SECOND FACTOR:");
        line(out, "    failing         %d", fixed.totals().get(Assessment.Status.FAILED));
        line(out, "    left to people  %s", ids(with(fixed, Assessment.Status.MANUAL)));
    }

    private static void act5Bill(StringBuilder out, List<Requirement> requirements) {
        line(out, "FIVE. The bill.");
        Level store = LevelChooser.choose(STORE).level();
        Level bank = LevelChooser.choose(BANK).level();
        SampleApp app = new SampleApp(SampleApp.Settings.store().withStaffMfa(true));
        line(out, "  THE SAME STORE, JUDGED AT TWO LEVELS:");
        line(out, "    level  applies  met  failed  manual");
        for (Level l : List.of(store, bank)) {
            Map<Assessment.Status, Integer> t = Assessment.of(requirements, l, app).totals();
            line(out, "    %-5s  %7d  %3d  %6d  %6d", l, requirements.size() - t.get(Assessment.Status.NOT_APPLICABLE),
                    t.get(Assessment.Status.MET), t.get(Assessment.Status.FAILED), t.get(Assessment.Status.MANUAL));
        }
        Assessment atBank = Assessment.of(requirements, bank, app);
        line(out, "  NEEDS A PERSON AT %s, AND NO TEST REPLACES IT:", bank);
        for (Requirement r : with(atBank, Assessment.Status.MANUAL)) {
            line(out, "    %s  %s", r.id(), r.statement());
        }
        line(out, "  MAPPED TO AN OLDER EDITION THAN %s:", Catalogue.EDITION);
        requirements.stream().filter(r -> !r.edition().equals(Catalogue.EDITION))
                .forEach(r -> line(out, "    %s  edition %s  <- REVIEW THE MAPPING", r.id(), r.edition()));
        line(out, "  WHAT A PASSING REPORT DOES NOT SAY:");
        line(out, "    which threats this system faces: the threat model does");
        line(out, "    that the store is secure: only that these checks pass");
    }
}
