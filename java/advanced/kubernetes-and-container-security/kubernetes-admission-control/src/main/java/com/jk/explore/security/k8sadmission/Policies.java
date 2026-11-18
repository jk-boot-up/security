package com.jk.explore.security.k8sadmission;

import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What each policy file says about itself: its name, its action (Deny or Audit) and the message a refusal carries. */
public final class Policies {

    private static final Pattern NAME = Pattern.compile("(?m)^  name: (\\S+)");
    private static final Pattern ACTION = Pattern.compile("validationActions: \\[(\\w+)]");
    private static final Pattern MESSAGE = Pattern.compile("message: \"(.*)\"");

    /** One policy file. */
    public record Info(String name, String action, String message) {
    }

    private Policies() {
    }

    public static Info read(String file) throws IOException {
        String text = Files.readString(Kyverno.POLICIES.resolve(file + ".yaml"));
        return new Info(find(NAME, text), find(ACTION, text), find(MESSAGE, text));
    }

    static String find(Pattern p, String text) {
        Matcher m = p.matcher(text);
        if (!m.find()) {
            throw new IllegalArgumentException("not found: " + p);
        }
        return m.group(1);
    }
}
