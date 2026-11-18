package com.jk.explore.security.depconf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs the build story in five numbered acts and prints exact, repeatable results. Each act prints its input (THE BUILD ASKS FOR, THE SETUP, ...)
 * and its result (THE RESULT) in separate labelled sections, and works out the derived values (a digest, a version comparison, a lockfile check).
 * The "malicious" script is only text; nothing is run.
 */
public class DepConfusionDemo {
    static final String ATTACK = "send CI secrets to attacker.example";
    static final Set<String> INTERNAL = Set.of("shop-pricing", "shop-audit", "shop-newlib");
    private static final int WIDTH = 66;

    public static void main(String[] args) {
        System.out.print(run());
    }

    static Map<String, VersionRange> dependencies() {
        Map<String, VersionRange> d = new LinkedHashMap<>();
        d.put("json-lib", VersionRange.major(2));
        d.put("shop-pricing", VersionRange.atLeast(1));
        d.put("shop-audit", VersionRange.exactly("3.1.0"));
        return d;
    }

    static Repository privateRepo() {
        return new Repository("company-private", false)
                .publish(Artifact.of("shop-pricing", "1.4.2", "internal pricing rules"))
                .publish(Artifact.of("shop-audit", "3.1.0", "internal audit client"));
    }

    static Repository publicRepo() {
        return new Repository("public-registry", true).publish(Artifact.of("json-lib", "2.0.1", "a JSON library"));
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Repository internal = privateRepo();
        Repository open = publicRepo();
        List<Repository> repos = List.of(internal, open);

        line(out, "ONE. A build that works.");
        line(out, "  THE BUILD ASKS FOR (a name, versions from and below):");
        dependencies().forEach((n, r) -> line(out, "    %-13s %s", n, r));
        line(out, "  THE REPOSITORIES (what each one holds):");
        for (Repository r : repos) {
            line(out, "    %-16s %s", r.name(), r.artifacts().stream().map(a -> a.name() + " " + a.version()).collect(Collectors.joining(", ")));
        }
        Build.Report good = new Build(new HighestVersionResolver(), repos).run(dependencies());
        result(out, "the highest version in any repository", good);
        Lockfile lock = new Lockfile();
        good.installed().forEach(lock::record);
        line(out, "  THE LOCKFILE (%d entries: version, SHA-256 digest):", lock.size());
        good.installed().forEach(r -> line(out, "    %-13s %-6s %s...", r.artifact().name(), r.artifact().version(), shortDigest(r.artifact())));
        Artifact pricing = good.installed().get(1).artifact();
        line(out, "  HOW A DIGEST IS MADE (%s %s):", pricing.name(), pricing.version());
        String[] parts = pricing.digestInput().split("\n", -1);
        String[] what = {"name@version", "the contents", "the install script: none"};
        for (int i = 0; i < 3; i++) {
            line(out, "    line %d   %-23s %s", i + 1, parts[i], what[i]);
        }
        line(out, "    bytes    %d of UTF-8 text, lines joined by a newline", pricing.digestInput().getBytes(StandardCharsets.UTF_8).length);
        line(out, "    SHA-256  %s", pricing.digest().substring(0, 32));
        line(out, "             %s", pricing.digest().substring(32));

        line(out, "TWO. Somebody registers the store's name in the public registry.");
        Artifact squat = Artifact.of("shop-pricing", "99.0.0", "looks like a pricing library").withInstallScript(ATTACK);
        open.publish(squat);
        open.publish(Artifact.of("shop-newlib", "1.0.0", "a name the online store has not published yet").withInstallScript(ATTACK));
        line(out, "  THE ATTACKER PUBLISHES (to public-registry, knowing only names):");
        open.artifacts().stream().filter(a -> a.installScript() != null)
                .forEach(a -> line(out, "    %-13s %-7s with an install script", a.name(), a.version()));
        line(out, "    the script: %s", ATTACK);
        VersionRange want = dependencies().get("shop-pricing");
        line(out, "  HOW THE HIGHEST VERSION IS CHOSEN (shop-pricing %s):", want);
        for (Repository r : repos) {
            Version v = r.find("shop-pricing", want).getFirst().version();
            line(out, "    %-16s %-7s in range: %s", r.name(), v, want.contains(v) ? "yes" : "no");
        }
        line(out, "    compare          %s, so %s is higher", squat.version().whyHigher(pricing.version()), squat.version());
        result(out, "the highest version in any repository", new Build(new HighestVersionResolver(), repos).run(dependencies()));

        line(out, "THREE. Repository order helps only until a name is missing.");
        line(out, "  THE SETUP (company-private first, then public-registry):");
        line(out, "    the dependencies as before");
        result(out, "the first repository that has the name", new Build(new FirstRepositoryResolver(), repos).run(dependencies()));
        Map<String, VersionRange> withNew = dependencies();
        withNew.put("shop-newlib", VersionRange.any());
        line(out, "  THE SETUP (a new internal library, shop-newlib %s):", withNew.get("shop-newlib"));
        for (Repository r : repos) {
            List<Artifact> found = r.find("shop-newlib", VersionRange.any());
            line(out, "    %-16s %s", r.name(), found.isEmpty() ? "has no shop-newlib yet" : "has shop-newlib " + found.getFirst().version() + " (the attacker's)");
        }
        result(out, "the name falls through to the next repository", new Build(new FirstRepositoryResolver(), repos).run(withNew));
        Repository squatted = publicRepo().publish(Artifact.of("shop-audit", "3.1.0", "same version number as ours").withInstallScript(ATTACK));
        line(out, "  THE SETUP (public-registry first, a common misconfiguration):");
        line(out, "    the store pins shop-audit %s", dependencies().get("shop-audit"));
        line(out, "    the attacker publishes shop-audit 3.1.0, the same number");
        result(out, "the first repository that has the name", new Build(new FirstRepositoryResolver(), List.of(squatted, internal)).run(dependencies()));

        line(out, "FOUR. Names decide where a package may come from.");
        ContentFilteringResolver filtering = new ContentFilteringResolver(n -> n.startsWith("shop-"));
        line(out, "  THE RULE (a name that starts with shop- is internal):");
        for (String n : withNew.keySet()) {
            line(out, "    %-13s %-9s -> %s only", n, filtering.isInternal(n) ? "internal" : "public",
                    filtering.allowedFor(n, repos).stream().map(Repository::name).collect(Collectors.joining(", ")));
        }
        result(out, "the dependencies as before", new Build(filtering, repos).run(dependencies()));
        result(out, "with the unpublished shop-newlib", new Build(filtering, repos).run(withNew));

        line(out, "FIVE. Locked digests, and watching the names.");
        Artifact changed = Artifact.of("shop-pricing", "1.4.2", "internal pricing rules, changed").withInstallScript(ATTACK);
        Repository tamperedPrivate = new Repository("company-private", false)
                .publish(changed)
                .publish(Artifact.of("shop-audit", "3.1.0", "internal audit client"));
        Lockfile.Entry locked = lock.get("shop-pricing");
        line(out, "  HOW THE LOCKFILE CHECK IS MADE (shop-pricing):");
        line(out, "    %-16s %-7s %s...", "the lockfile", locked.version(), locked.digest().substring(0, 16));
        line(out, "    %-16s %-7s %s...  %s", "public-registry", squat.version(), shortDigest(squat), verdict(locked, squat));
        line(out, "    %-16s %-7s %s...  %s", "changed private", changed.version(), shortDigest(changed), verdict(locked, changed));
        Resolver verifying = new VerifyingResolver(new HighestVersionResolver(), lock);
        result(out, "the highest version, checked against the lockfile", new Build(verifying, repos).run(dependencies()));
        result(out, "names decide, but company-private's 1.4.2 changed",
                new Build(new VerifyingResolver(filtering, lock), List.of(tamperedPrivate, open)).run(dependencies()));
        line(out, "  THE NAME WATCH (does the public registry have our names?):");
        line(out, "    internal names  %s", String.join(", ", INTERNAL.stream().sorted().toList()));
        line(out, "    also public     %s", String.join(", ", NameWatch.shadowedNames(INTERNAL, open)));
        return out.toString();
    }

    private static String shortDigest(Artifact a) {
        return a.digest().substring(0, 16);
    }

    private static String verdict(Lockfile.Entry expected, Artifact found) {
        if (!found.version().equals(expected.version())) {
            return "version differs";
        }
        return found.digest().equals(expected.digest()) ? "matches" : "digest differs";
    }

    private static void result(StringBuilder out, String how, Build.Report r) {
        line(out, "  THE RESULT (%s):", how);
        r.installed().forEach(x -> line(out, "    installed %s", x));
        r.scriptsRun().forEach(s -> {
            String[] p = s.split(": ", 2);
            line(out, "    INSTALL SCRIPT RAN for %s (the attacker's code):", p[0]);
            line(out, "      %s", p[1]);
        });
        r.errors().forEach(e -> wrap(out, "    refused: " + e));
    }

    /** Breaks a long line at spaces so no output line is wider than {@value #WIDTH} characters. */
    private static void wrap(StringBuilder out, String text) {
        List<String> lines = new ArrayList<>();
        String rest = text;
        while (rest.length() > WIDTH) {
            int cut = rest.lastIndexOf(' ', WIDTH);
            lines.add(rest.substring(0, cut));
            rest = "      " + rest.substring(cut + 1);
        }
        lines.add(rest);
        lines.forEach(l -> line(out, "%s", l));
    }

    private static void line(StringBuilder out, String format, Object... args) {
        out.append(String.format(format, args)).append('\n');
    }
}
