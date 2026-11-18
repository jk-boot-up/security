package com.jk.explore.security.policyascode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Switches off one deny rule at a time and asks whether the tests notice. */
public final class Mutants {

    private static final Pattern RULE = Pattern.compile("(?m)^deny contains ");

    private Mutants() {
    }

    public static int count(String source) {
        Matcher m = RULE.matcher(source);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** The source with the n-th deny rule renamed, so that it never applies. */
    public static String disable(String source, int n) {
        Matcher m = RULE.matcher(source);
        for (int i = 0; m.find(); i++) {
            if (i == n) {
                return source.substring(0, m.start()) + "deny_off contains " + source.substring(m.end());
            }
        }
        throw new IllegalArgumentException("no rule " + n);
    }

    /** The comment line just above the n-th deny rule, such as "R5: a cpu limit". */
    public static String label(String source, int n) {
        Matcher m = RULE.matcher(source);
        for (int i = 0; m.find(); i++) {
            if (i == n) {
                String before = source.substring(0, m.start()).stripTrailing();
                return before.substring(before.lastIndexOf('\n') + 1).replaceFirst("^#\\s*", "");
            }
        }
        throw new IllegalArgumentException("no rule " + n);
    }

    /** How many tests were selected, and for each mutant whether one of them failed (the mutant is noticed). */
    public record Score(int tests, List<Boolean> noticed) {

        public int killed() {
            return (int) noticed.stream().filter(b -> b).count();
        }
    }

    /** Runs the selected tests (null: all) on every mutant of the policy in that folder. */
    public static Score score(Path policyDir, String regex, Path work) throws IOException, InterruptedException {
        String source = Files.readString(policyDir.resolve("main.rego"));
        List<Boolean> noticed = new ArrayList<>();
        for (int i = 0; i < count(source); i++) {
            Path dir = Files.createDirectories(work.resolve("mutant"));
            Files.writeString(dir.resolve("main.rego"), disable(source, i));
            Files.copy(policyDir.resolve("main_test.rego"), dir.resolve("main_test.rego"), StandardCopyOption.REPLACE_EXISTING);
            Opa.Tests t = Opa.test(regex, List.of(dir));
            noticed.add(t.passed() != t.total() || t.total() < 0);
        }
        return new Score(Opa.test(regex, List.of(policyDir.resolve("main.rego"), policyDir.resolve("main_test.rego"))).total(), noticed);
    }
}
