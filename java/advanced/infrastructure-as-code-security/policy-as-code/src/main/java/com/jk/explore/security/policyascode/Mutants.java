package com.jk.explore.security.policyascode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /** How many of the mutants make the selected tests fail. */
    public static int killed(Path policyDir, String regex, Path work) throws IOException, InterruptedException {
        String source = Files.readString(policyDir.resolve("main.rego"));
        int killed = 0;
        for (int i = 0; i < count(source); i++) {
            Path dir = Files.createDirectories(work.resolve("mutant"));
            Files.writeString(dir.resolve("main.rego"), disable(source, i));
            Files.copy(policyDir.resolve("main_test.rego"), dir.resolve("main_test.rego"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Opa.Tests t = Opa.test(regex, List.of(dir));
            if (t.passed() != t.total() || t.total() < 0) {
                killed++;
            }
        }
        return killed;
    }
}
