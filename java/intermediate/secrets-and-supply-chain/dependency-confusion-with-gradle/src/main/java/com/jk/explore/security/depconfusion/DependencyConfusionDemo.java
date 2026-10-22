package com.jk.explore.security.depconfusion;

import java.io.IOException;

/** Real Gradle builds against a private and a public repository: who answers for a name, what a filter and a checksum refuse, and what they do not. */
public class DependencyConfusionDemo {

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

    private static void act1(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "ONE. A build that works.");
        line(out, "  the build asks the private repository first and the public one second; it resolved " + lab.build("one", both(lab, "", ""), Lab.DEPENDENCIES).run());
    }

    private static void act2(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "TWO. Somebody registers the company's name in the public repository.");
        lab.publicRepo.publish("com.shop", "pricing", "99.0.0", "public (the attacker's)");
        line(out, "  the dependency asks for pricing in the range [1.4,); gradle lists the versions of every repository and takes the highest: " + lab.build("two", both(lab, "", ""), Lab.DEPENDENCIES).run());
        lab.publicRepo.publish("com.shop", "audit", "3.1.0", "public (the attacker's)");
        line(out, "  an exact version is safer, but with the public repository declared first, the attacker's copy of audit 3.1.0 is the first one found: "
                + lab.build("two-b", Lab.maven(lab.publicRepo, "") + "\n" + Lab.maven(lab.privateRepo, ""), Lab.DEPENDENCIES).run());
    }

    private static void act3(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "THREE. The repository says which names it may answer for.");
        String privateOnly = "                includeGroupAndSubgroups 'com.shop'";
        String notShop = "                excludeGroupAndSubgroups 'com.shop'";
        lab.publicRepo.clearRequests();
        line(out, "  the private repository includes group com.shop and the public one excludes it: " + lab.build("three", both(lab, privateOnly, notShop), Lab.DEPENDENCIES).run());
        line(out, "  requests that the public repository received for com.shop: " + lab.publicRepo.requestsUnder("com/shop"));
        lab.publicRepo.clearRequests();
        Build.Result unpublished = lab.build("three-b", both(lab, privateOnly, notShop), "        cp 'com.shop:newlib:1.0'").run();
        line(out, "  an internal library that is not published yet: " + unpublished + "; requests the public repository received for it: " + lab.publicRepo.requestsUnder("com/shop"));
        lab.publicRepo.clearRequests();
        lab.build("three-c", both(lab, "", ""), "        cp 'com.shop:newlib:1.0'").run();
        line(out, "  without the filter the same build asks the public repository for the internal name: " + (lab.publicRepo.requestsUnder("com/shop") > 0) + "  <- THE NAME LEAKS, AND SO DOES THE CHANCE TO ANSWER IT");
    }

    private static void act4(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Checksums that the build checks.");
        try (Lab lab = new Lab()) {
            Build good = lab.build("four", both(lab, "", ""), Lab.DEPENDENCIES);
            Build.Result written = good.run("--write-verification-metadata", "sha256");
            line(out, "  the checksums of what was resolved are written once: " + written.ok() + ", and kept in gradle/verification-metadata.xml next to the build file");
            line(out, "  the same build with the metadata: " + good.run());
            lab.publicRepo.publish("com.shop", "pricing", "99.0.0", "public (the attacker's)");
            line(out, "  the attacker's 99.0.0 now wins the version range, and the build: " + good.run());
            lab.publicRepo.clearRequests();
        }
        try (Lab lab = new Lab()) {
            Build good = lab.build("four-c", both(lab, "", ""), Lab.DEPENDENCIES);
            good.run("--write-verification-metadata", "sha256");
            lab.privateRepo.publish("com.shop", "pricing", "1.4.2", "private (replaced)");
            line(out, "  the same version number with other bytes in the private repository: " + good.run());
        }
    }

    private static void act5(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        try (Lab lab = new Lab()) {
            lab.publicRepo.publish("com.shop.tools", "signer", "9.0", "public (the attacker's)");
            String tools = "        cp 'com.shop.tools:signer:[1.0,)'";
            String exactGroupOnly = "                includeGroup 'com.shop'";
            String excludeExact = "                excludeGroup 'com.shop'";
            line(out, "  a filter for the exact group com.shop does not cover the group com.shop.tools: " + lab.build("five", both(lab, exactGroupOnly, excludeExact), tools).run() + "  <- A FILTER IS ONLY AS GOOD AS ITS LIST");
            line(out, "  with the group and its subgroups: " + lab.build("five-b", both(lab, "                includeGroupAndSubgroups 'com.shop'", "                excludeGroupAndSubgroups 'com.shop'"), tools).run());
        }
        try (Lab lab = new Lab()) {
            lab.publicRepo.publish("com.shop", "pricing", "99.0.0", "public (the attacker's)");
            Build late = lab.build("five-c", both(lab, "", ""), Lab.DEPENDENCIES);
            late.run("--write-verification-metadata", "sha256");
            line(out, "  checksums written while the attacker's package was already being resolved are the attacker's checksums: " + late.run() + "  <- WRITE THEM ONLY FROM A BUILD YOU TRUST, AND REVIEW EVERY CHANGE");
        }
        line(out, "  the repositories of the plugins and of the build script are a separate list (pluginManagement), and need the same filters");
        line(out, "  the repositories here are plain http on loopback; a real one is https, and the build refuses http unless it is told to allow it");
    }
}
