package com.jk.explore.security.depconf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Runs the build story in five numbered acts and prints exact, repeatable results. The "malicious" script is only text; nothing is run. */
public class DepConfusionDemo {

    static final String ATTACK = "send CI secrets to attacker.example";
    static final Set<String> INTERNAL = Set.of("shop-pricing", "shop-audit", "shop-newlib");

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

        out.append("== 1. A build that works ==\n");
        Build.Report good = new Build(new HighestVersionResolver(), repos).run(dependencies());
        good.installed().forEach(r -> out.append("  ").append(r).append('\n'));
        Lockfile lock = new Lockfile();
        good.installed().forEach(lock::record);
        out.append("the lockfile records ").append(lock.size()).append(" dependencies with their digests\n");

        out.append("\n== 2. Somebody registers the online store's package name in the public registry ==\n");
        open.publish(Artifact.of("shop-pricing", "99.0.0", "looks like a pricing library").withInstallScript(ATTACK));
        open.publish(Artifact.of("shop-newlib", "1.0.0", "a name the online store has not published yet").withInstallScript(ATTACK));
        out.append("the highest version in any repository wins:\n");
        show(out, new Build(new HighestVersionResolver(), repos).run(dependencies()));

        out.append("\n== 3. Repository order helps only until a name is missing ==\n");
        out.append("private repository first, dependencies as before:\n");
        show(out, new Build(new FirstRepositoryResolver(), repos).run(dependencies()));
        Map<String, VersionRange> withNew = dependencies();
        withNew.put("shop-newlib", VersionRange.any());
        out.append("a new internal library that is not in the private repository yet:\n");
        show(out, new Build(new FirstRepositoryResolver(), repos).run(withNew));
        Repository squatted = publicRepo().publish(Artifact.of("shop-audit", "3.1.0", "same version number as ours").withInstallScript(ATTACK));
        out.append("public registry first (a common misconfiguration), and the attacker copies an exact version number:\n");
        show(out, new Build(new FirstRepositoryResolver(), List.of(squatted, internal)).run(dependencies()));

        out.append("\n== 4. Names decide where a package may come from ==\n");
        Resolver filtering = new ContentFilteringResolver(n -> n.startsWith("shop-"));
        out.append("dependencies as before:\n");
        show(out, new Build(filtering, repos).run(dependencies()));
        out.append("the unpublished internal library:\n");
        show(out, new Build(filtering, repos).run(withNew));

        out.append("\n== 5. Locked digests, and watching the names ==\n");
        Resolver verifying = new VerifyingResolver(new HighestVersionResolver(), lock);
        out.append("highest-version resolver with the lockfile:\n");
        show(out, new Build(verifying, repos).run(dependencies()));
        Repository tamperedPrivate = new Repository("company-private", false)
                .publish(Artifact.of("shop-pricing", "1.4.2", "internal pricing rules, changed").withInstallScript(ATTACK))
                .publish(Artifact.of("shop-audit", "3.1.0", "internal audit client"));
        out.append("same version number, different bytes in the private repository:\n");
        show(out, new Build(new VerifyingResolver(filtering, lock), List.of(tamperedPrivate, open)).run(dependencies()));
        out.append("internal names that the public registry also has -> ").append(NameWatch.shadowedNames(INTERNAL, open)).append('\n');
        return out.toString();
    }

    private static void show(StringBuilder out, Build.Report r) {
        r.installed().forEach(x -> out.append("  installed ").append(x).append('\n'));
        r.scriptsRun().forEach(s -> out.append("  INSTALL SCRIPT RAN -> ").append(s).append("  <- ATTACKER'S CODE ON THE BUILD MACHINE\n"));
        r.errors().forEach(e -> out.append("  refused: ").append(e).append('\n'));
    }
}
