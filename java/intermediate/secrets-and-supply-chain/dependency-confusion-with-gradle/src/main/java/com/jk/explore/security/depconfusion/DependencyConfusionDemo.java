package com.jk.explore.security.depconfusion;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Real Gradle builds against a private and a public repository: who answers for a name, what a filter and a checksum refuse, and what they do not.
 * Each act prints its input (THE BUILD, THE FILTERS, ...) and its result (THE RESULT) in separate labelled sections, and works out the derived
 * values: the versions in a range, the requests a repository saw, the checksums Gradle compares, the groups a filter covers.
 */
public class DependencyConfusionDemo {
    static final String ATTACKER = "public (the attacker's)";
    private static final String PRIVATE_ONLY = "includeGroupAndSubgroups 'com.shop'";
    private static final String NOT_SHOP = "excludeGroupAndSubgroups 'com.shop'";
    private static final String NEWLIB = "        cp 'com.shop:newlib:1.0'";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Lab lab = new Lab()) {
            act1(out, lab);
            act2(out, lab);
            act3(out, lab);
        }
        act4(out);
        act5(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String both(Lab lab, String privateFilter, String publicFilter) {
        return Lab.maven(lab.privateRepo, privateFilter) + "\n" + Lab.maven(lab.publicRepo, publicFilter);
    }

    /** THE RESULT: one line per resolved dependency (name, then who published the jar), or the reason the build refused. */
    private static void result(StringBuilder out, String how, Build.Result r) {
        line(out, "  THE RESULT (" + how + "):");
        if (!r.ok()) {
            line(out, "    refused: " + r.failure());
            return;
        }
        for (String x : r.resolved()) {
            String[] p = x.split(" from ", 2);
            line(out, "    %-26s from %s".formatted(p[0], p[1]));
        }
    }

    private static List<String> dependencies() {
        return Lab.DEPENDENCIES.lines().map(l -> l.strip().replace("cp '", "").replace("'", "")).toList();
    }

    private static void act1(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "ONE. A build that works.");
        line(out, "  THE REPOSITORIES (settings.gradle, asked in this order):");
        line(out, "    1. " + lab.privateRepo.name() + "   the store's own; only the store publishes");
        line(out, "    2. " + lab.publicRepo.name() + "    anyone can publish");
        line(out, "  THE DEPENDENCIES (build.gradle):");
        dependencies().forEach(d -> line(out, "    " + d));
        result(out, "what Gradle resolved, and who published each jar", lab.build("one", both(lab, "", ""), Lab.DEPENDENCIES).run());
    }

    private static void act2(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "TWO. Somebody registers the store's name in the public repository.");
        lab.publicRepo.publish("com.shop", "pricing", "99.0.0", ATTACKER);
        line(out, "  THE ATTACKER PUBLISHES (to public, knowing only the name):");
        line(out, "    com.shop:pricing:99.0.0");
        String range = "[1.4,)";
        line(out, "  HOW GRADLE PICKS A VERSION (com.shop:pricing:" + range + "):");
        List<String> all = new ArrayList<>();
        for (Repo r : List.of(lab.privateRepo, lab.publicRepo)) {
            List<String> listed = r.versions("com.shop", "pricing");
            all.addAll(listed);
            line(out, "    %-8s lists %s in maven-metadata.xml".formatted(r.name(), String.join(", ", listed)));
        }
        line(out, "    at or above %s: %s".formatted(VersionSteps.lowerBound(range),
                String.join(", ", all.stream().filter(v -> VersionSteps.inRange(v, range)).sorted(VersionSteps.ORDER).toList())));
        String top = VersionSteps.highest(all, range).orElseThrow();
        String other = all.stream().filter(v -> !v.equals(top)).findFirst().orElseThrow();
        line(out, "    the highest: " + top + " (" + VersionSteps.firstDifference(top, other) + ", number by number)");
        result(out, "a range takes the highest of every repository", lab.build("two", both(lab, "", ""), Lab.DEPENDENCIES).run());
        lab.publicRepo.publish("com.shop", "audit", "3.1.0", ATTACKER);
        line(out, "  THE SETUP (public declared first; the attacker copies audit):");
        line(out, "    the store pins com.shop:audit:3.1.0, an exact version");
        line(out, "    public now has com.shop:audit:3.1.0 too, the same number");
        result(out, "the first repository that has it answers",
                lab.build("two-b", Lab.maven(lab.publicRepo, "") + "\n" + Lab.maven(lab.privateRepo, ""), Lab.DEPENDENCIES).run());
    }

    private static void act3(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "THREE. The repository says which names it may answer for.");
        line(out, "  THE FILTERS (content { } in settings.gradle):");
        line(out, "    private  " + PRIVATE_ONLY.strip());
        line(out, "    public   " + NOT_SHOP.strip());
        lab.publicRepo.clearRequests();
        result(out, "the attacker's packages are still in public", lab.build("three", both(lab, PRIVATE_ONLY, NOT_SHOP), Lab.DEPENDENCIES).run());
        line(out, "  THE PUBLIC REPOSITORY'S LOG (requests for com/shop/...):");
        line(out, "    " + lab.publicRepo.requestsUnder("com/shop") + " requests");
        lab.publicRepo.clearRequests();
        line(out, "  THE SETUP (an internal library not published yet):");
        line(out, "    " + NEWLIB.strip().replace("cp '", "").replace("'", "") + ", in neither repository");
        result(out, "with the filters", lab.build("three-b", both(lab, PRIVATE_ONLY, NOT_SHOP), NEWLIB).run());
        line(out, "    the public repository received " + lab.publicRepo.requestsUnder("com/shop") + " requests for com/shop");
        lab.publicRepo.clearRequests();
        lab.build("three-c", both(lab, "", ""), NEWLIB).run();
        line(out, "  WITHOUT THE FILTERS (the same build asks public for the name):");
        lab.publicRepo.pathsUnder("com/shop").forEach(p -> line(out, "    GET " + p));
        line(out, "    <- THE NAME LEAKS, AND SO DOES THE CHANCE TO ANSWER IT");
    }

    private static void act4(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Checksums that the build checks.");
        try (Lab lab = new Lab()) {
            Build good = lab.build("four", both(lab, "", ""), Lab.DEPENDENCIES);
            Build.Result written = good.run("--write-verification-metadata", "sha256");
            String xml = Files.readString(good.dir().resolve("gradle").resolve("verification-metadata.xml"));
            String listed = Checksums.listed(xml, "com.shop", "pricing", "1.4.2", "pricing-1.4.2.jar");
            String computed = Checksums.sha256(lab.privateRepo.jarBytes("com.shop", "pricing", "1.4.2"));
            line(out, "  THE METADATA (--write-verification-metadata sha256, once):");
            line(out, "    written: " + written.ok() + ", to gradle/verification-metadata.xml");
            line(out, "    pricing-1.4.2.jar  sha256 " + listed.substring(0, 16) + "...");
            line(out, "    the SHA-256 of private's jar, computed here: " + (listed.equals(computed) ? "the same" : "different"));
            result(out, "the same build, checked", good.run());
            lab.publicRepo.publish("com.shop", "pricing", "99.0.0", ATTACKER);
            String attacker = Checksums.sha256(lab.publicRepo.jarBytes("com.shop", "pricing", "99.0.0"));
            line(out, "  HOW THE CHECK IS MADE (com.shop:pricing, the jar):");
            line(out, "    %-17s %-7s %s...".formatted("the metadata", "1.4.2", listed.substring(0, 16)));
            line(out, "    %-17s %-7s %s...  %s".formatted("the attacker's", "99.0.0", attacker.substring(0, 16),
                    Checksums.listed(xml, "com.shop", "pricing", "99.0.0", "pricing-99.0.0.jar") == null ? "not listed" : "listed"));
            result(out, "the attacker's 99.0.0 wins the range", good.run());
        }
        try (Lab lab = new Lab()) {
            Build good = lab.build("four-c", both(lab, "", ""), Lab.DEPENDENCIES);
            good.run("--write-verification-metadata", "sha256");
            String xml = Files.readString(good.dir().resolve("gradle").resolve("verification-metadata.xml"));
            String listed = Checksums.listed(xml, "com.shop", "pricing", "1.4.2", "pricing-1.4.2.jar");
            lab.privateRepo.publish("com.shop", "pricing", "1.4.2", "private (replaced)");
            String replaced = Checksums.sha256(lab.privateRepo.jarBytes("com.shop", "pricing", "1.4.2"));
            line(out, "  THE SETUP (private's pricing 1.4.2 is replaced, other bytes):");
            line(out, "    %-17s %-7s %s...  %s".formatted("the replaced jar", "1.4.2", replaced.substring(0, 16),
                    replaced.equals(listed) ? "the same" : "differs"));
            result(out, "the same version number, other bytes", good.run());
        }
    }

    private static void act5(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        try (Lab lab = new Lab()) {
            lab.publicRepo.publish("com.shop.tools", "signer", "9.0", ATTACKER);
            String tools = "        cp 'com.shop.tools:signer:[1.0,)'";
            line(out, "  THE SETUP (the attacker publishes com.shop.tools:signer:9.0):");
            line(out, "    the store asks for com.shop.tools:signer:[1.0,)");
            line(out, "  HOW A FILTER MATCHES (does it cover the group com.shop.tools?):");
            for (String rule : List.of("includeGroup", "includeGroupAndSubgroups")) {
                line(out, "    %-37s %s".formatted(rule + " 'com.shop'", GroupFilter.covers(rule, "com.shop", "com.shop.tools") ? "yes" : "no"));
            }
            result(out, "includeGroup and excludeGroup 'com.shop'",
                    lab.build("five", both(lab, "                includeGroup 'com.shop'", "                excludeGroup 'com.shop'"), tools).run());
            line(out, "    <- A FILTER IS ONLY AS GOOD AS ITS LIST");
            result(out, "the group and its subgroups", lab.build("five-b", both(lab, PRIVATE_ONLY, NOT_SHOP), tools).run());
        }
        try (Lab lab = new Lab()) {
            lab.publicRepo.publish("com.shop", "pricing", "99.0.0", ATTACKER);
            Build late = lab.build("five-c", both(lab, "", ""), Lab.DEPENDENCIES);
            late.run("--write-verification-metadata", "sha256");
            line(out, "  THE SETUP (checksums written after the attacker published):");
            line(out, "    --write-verification-metadata sha256, no filters");
            result(out, "the metadata trusts whatever was resolved", late.run());
            line(out, "    <- WRITE THEM ONLY FROM A BUILD YOU TRUST, REVIEW CHANGES");
        }
        line(out, "  STILL TO DO (not shown by a build here):");
        Stream.of("plugin repositories (pluginManagement) need the same filters",
                "the lab serves http on loopback; a real repository uses https")
                .forEach(s -> line(out, "    " + s));
    }
}
