package com.jk.explore.security.container;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the container story in five numbered acts and prints exact, repeatable results. Each act prints its input (the settings or the command) and its result (the
 * attacks and the Pod Security level) in separate labelled sections, and works out the count and the level step by step.
 */
public class ContainerDemo {

    static final int WIDTH = 66;
    static final Map<String, String> SHORT = new LinkedHashMap<>();

    static {
        SHORT.put(Attacker.CONTAINER, "container");
        SHORT.put(Attacker.KERNEL, "kernel");
        SHORT.put(Attacker.HOST, "host");
        SHORT.put(Attacker.RESOURCES, "resources");
    }

    public static void main(String[] args) {
        System.out.print(run());
    }

    static ContainerSpec hardened() {
        return ContainerSpec.dockerDefaults().named("hardened").user("10001").dropAllCapabilities().readOnlyFilesystem().noNewPrivileges()
                .withLimits(512, 200).secretsAsFiles().withImage("shop-app@sha256:9f2c1a7d");
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        int all = Attacker.ATTACKS.size();

        line(out, "ONE. What 'docker run' with no options gives an attacker.");
        ContainerSpec defaults = ContainerSpec.dockerDefaults();
        line(out, "  THE SETTINGS (docker run " + defaults.image() + "):");
        settings(out, defaults);
        line(out, "  THE ATTACKS, BY WHAT THEY REACH:");
        for (String reach : SHORT.keySet()) {
            line(out, "    " + reach + ":");
            Attacker.run(defaults).stream().filter(o -> o.attack().reach().equals(reach)).forEach(o -> line(out, "      " + o));
        }
        line(out, "  HOW THE COUNT IS MADE:");
        count(out, defaults);
        line(out, "  HOW THE LEVEL IS MADE:");
        level(out, defaults);

        line(out, "TWO. The configuration from the tutorial.");
        ContainerSpec reckless = ContainerSpec.reckless();
        line(out, "  THE COMMAND:");
        Renderer.dockerLines(reckless).forEach(l -> line(out, "    " + l));
        line(out, "  THE SETTINGS IT GIVES:");
        settings(out, reckless);
        line(out, "  HOW THE COUNT IS MADE:");
        count(out, reckless);
        List<String> why = PodSecurityLevel.violations(reckless, PodSecurityLevel.BASELINE);
        line(out, "  WHY IT IS NOT EVEN BASELINE (" + why.size() + " reasons):");
        why.forEach(v -> line(out, "    " + v));
        line(out, "    level  " + PodSecurityLevel.of(reckless));

        line(out, "THREE. One setting at a time.");
        line(out, "  BEFORE THE STEPS:");
        ContainerSpec c = reckless;
        line(out, String.format("    %-29s %2d of %d  %s", "the tutorial configuration", Attacker.possibleIds(c).size(), all, PodSecurityLevel.of(c)));
        line(out, "  THE STEPS (one setting more each time):");
        c = step(out, 1, "no host access", c, c.withoutHostAccess());
        c = step(out, 2, "default seccomp profile", c, c.withSeccomp(Seccomp.RUNTIME_DEFAULT));
        c = step(out, 3, "drop all capabilities", c, c.dropAllCapabilities());
        c = step(out, 4, "a user that is not root", c, c.user("10001"));
        c = step(out, 5, "no new privileges", c, c.noNewPrivileges());
        c = step(out, 6, "read-only root, noexec /tmp", c, c.readOnlyFilesystem());
        c = step(out, 7, "memory and process limits", c, c.withLimits(512, 200));
        line(out, "  AFTER THE STEPS:");
        line(out, String.format("    %d of %d possible: %s", Attacker.possibleIds(c).size(), all, String.join(", ", Attacker.possibleIds(c))));

        line(out, "FOUR. The hardened container.");
        ContainerSpec h = hardened();
        line(out, "  THE COMMAND:");
        Renderer.dockerLines(h).forEach(l -> line(out, "    " + l));
        line(out, "  THE SAME SETTINGS FOR KUBERNETES:");
        Renderer.kubernetesSecurityContext(h).lines().forEach(l -> line(out, "    " + l));
        line(out, "  HOW THE LEVEL IS MADE:");
        level(out, h);
        line(out, "  WHAT IS STILL POSSIBLE:");
        Attacker.run(h).stream().filter(Outcome::possible).forEach(o -> line(out, "    " + o));

        line(out, "FIVE. Settings that look harmless.");
        line(out, "  BEFORE: the hardened container");
        summary(out, h);
        ContainerSpec netRaw = h.addCapability("NET_RAW");
        line(out, "  AFTER: hardened + NET_RAW");
        changed(out, h, netRaw, "sniff-network");
        ContainerSpec admin = h.user("root").addCapability("SYS_ADMIN");
        line(out, "  AFTER: hardened + root + SYS_ADMIN");
        changed(out, h, admin, null);
        return out.toString();
    }

    private static void settings(StringBuilder out, ContainerSpec c) {
        Renderer.settings(c).forEach(s -> line(out, String.format("    %-15s %s", s[0], s[1])));
    }

    /** The possible attacks counted by what they reach, then added up. */
    private static void count(StringBuilder out, ContainerSpec c) {
        List<Outcome> o = Attacker.run(c);
        List<String> parts = new ArrayList<>();
        SHORT.forEach((reach, name) -> parts.add(name + " " + o.stream().filter(x -> x.possible() && x.attack().reach().equals(reach)).count()));
        line(out, "    " + String.join(" + ", parts) + " = " + Attacker.possible(o) + " of " + o.size());
    }

    /** Every Pod Security check, baseline first, and the level that follows. */
    private static void level(StringBuilder out, ContainerSpec c) {
        for (PodSecurityLevel.Check k : PodSecurityLevel.CHECKS) {
            line(out, String.format("    %-11s %-27s %s", k.level().name().toLowerCase(), k.name(), k.passes(c) ? "pass" : "FAIL"));
        }
        PodSecurityLevel l = PodSecurityLevel.of(c);
        line(out, "    level       " + l + switch (l) {
            case PRIVILEGED -> " (a baseline check fails)";
            case BASELINE -> " (baseline passes, restricted fails)";
            case RESTRICTED -> " (every check passes)";
        });
    }

    private static ContainerSpec step(StringBuilder out, int n, String label, ContainerSpec before, ContainerSpec after) {
        List<String> blocked = new ArrayList<>(Attacker.possibleIds(before));
        blocked.removeAll(Attacker.possibleIds(after));
        line(out, String.format("    %d %-27s %2d of %d  %s", n, label, Attacker.possibleIds(after).size(), Attacker.ATTACKS.size(), PodSecurityLevel.of(after)));
        wrap(out, "        blocks ", blocked.isEmpty() ? List.of("nothing") : blocked);
        return after;
    }

    private static void summary(StringBuilder out, ContainerSpec c) {
        line(out, String.format("    %d of %d possible, level %s", Attacker.possibleIds(c).size(), Attacker.ATTACKS.size(), PodSecurityLevel.of(c)));
    }

    /** What one added setting changes: the count, the level, the attacks it opens and the check it fails. */
    private static void changed(StringBuilder out, ContainerSpec before, ContainerSpec after, String watch) {
        summary(out, after);
        List<String> opened = new ArrayList<>(Attacker.possibleIds(after));
        opened.removeAll(Attacker.possibleIds(before));
        wrap(out, "    newly possible: ", opened.isEmpty() ? List.of("nothing") : opened);
        if (watch != null) {
            Attacker.run(after).stream().filter(o -> o.attack().id().equals(watch)).forEach(o -> line(out, "    " + o));
        }
        PodSecurityLevel.CHECKS.stream().filter(k -> !k.passes(after) && k.passes(before))
                .forEach(k -> line(out, String.format("    now FAIL  %-11s %s", k.level().name().toLowerCase(), k.name())));
    }

    /** Prints a comma-separated list after a prefix, wrapped under the first item at {@link #WIDTH} characters. */
    static void wrap(StringBuilder out, String prefix, List<String> items) {
        StringBuilder cur = new StringBuilder(prefix);
        String indent = " ".repeat(prefix.length());
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i) + (i < items.size() - 1 ? "," : "");
            if (cur.length() > prefix.length() && cur.length() + 1 + item.length() > WIDTH) {
                line(out, cur.toString());
                cur = new StringBuilder(indent);
            } else if (cur.length() > prefix.length()) {
                cur.append(' ');
            }
            cur.append(item);
        }
        line(out, cur.toString());
    }

    private static void line(StringBuilder out, String s) {
        out.append(s).append('\n');
    }
}
