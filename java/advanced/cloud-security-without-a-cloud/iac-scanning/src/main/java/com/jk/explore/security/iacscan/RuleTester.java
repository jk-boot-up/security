package com.jk.explore.security.iacscan;

import java.util.List;

/** Tests a rule with its own two samples: the bad one must fire it, the good one must not. A sample starting with "pod: " goes in the pod spec, any other in the container. */
public final class RuleTester {

    private RuleTester() {
    }

    static String wrap(String sample) {
        String pod = "";
        String container = "";
        if (sample.startsWith("pod: ")) {
            pod = "  " + sample.substring(5) + "\n";
        } else {
            container = "      " + sample + "\n";
        }
        return "apiVersion: v1\nkind: Pod\nmetadata: {name: t}\nspec:\n" + pod + "  containers:\n    - name: c\n      image: x:1\n" + container;
    }

    static boolean fires(Rule rule, String sample) {
        return !Engine.scan(wrap(sample), List.of(rule)).isEmpty();
    }

    /** What is wrong with the rule, or an empty text when it passes. */
    public static String check(Rule rule) {
        if (!fires(rule, rule.bad())) {
            return rule.id() + " does not fire on its bad sample";
        }
        if (fires(rule, rule.good())) {
            return rule.id() + " fires on its good sample";
        }
        return "";
    }
}
