package com.jk.explore.security.depconfusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real Gradle builds against two repositories on loopback ports. Run with ./gradlew integrationTest. */
@Tag("integration")
class DependencyConfusionTest {

    private static final String SHOP = "                includeGroupAndSubgroups 'com.shop'";
    private static final String NOT_SHOP = "                excludeGroupAndSubgroups 'com.shop'";

    private static String both(Lab lab, String privateFilter, String publicFilter) {
        return Lab.maven(lab.privateRepo, privateFilter) + "\n" + Lab.maven(lab.publicRepo, publicFilter);
    }

    @Test
    void anHonestBuildTakesInternalNamesFromThePrivateRepository() throws Exception {
        try (Lab lab = new Lab()) {
            Build.Result r = lab.build("t1", both(lab, "", ""), Lab.DEPENDENCIES).run();
            assertTrue(r.resolvedFrom("com.shop:pricing:1.4.2", "private"), r.toString());
            assertTrue(r.resolvedFrom("org.acme:json-lib:2.0.1", "public"));
        }
    }

    @Test
    void aVersionRangeTakesTheHighestVersionOfAnyRepository() throws Exception {
        try (Lab lab = new Lab()) {
            lab.publicRepo.publish("com.shop", "pricing", "99.0.0", "public (the attacker's)");
            Build.Result r = lab.build("t2", both(lab, "", ""), Lab.DEPENDENCIES).run();
            assertTrue(r.resolvedFrom("com.shop:pricing:99.0.0", "public (the attacker's)"), r.toString());
        }
    }

    @Test
    void anExactVersionComesFromTheFirstRepositoryThatHasIt() throws Exception {
        try (Lab lab = new Lab()) {
            lab.publicRepo.publish("com.shop", "audit", "3.1.0", "public (the attacker's)");
            Build.Result publicFirst = lab.build("t3a", Lab.maven(lab.publicRepo, "") + "\n" + Lab.maven(lab.privateRepo, ""), Lab.DEPENDENCIES).run();
            Build.Result privateFirst = lab.build("t3b", both(lab, "", ""), Lab.DEPENDENCIES).run();
            assertTrue(publicFirst.resolvedFrom("com.shop:audit:3.1.0", "public (the attacker's)"));
            assertTrue(privateFirst.resolvedFrom("com.shop:audit:3.1.0", "private"));
        }
    }

    @Test
    void aContentFilterKeepsTheAttackersPackageOutAndTheInternalNameAtHome() throws Exception {
        try (Lab lab = new Lab()) {
            lab.publicRepo.publish("com.shop", "pricing", "99.0.0", "public (the attacker's)");
            lab.publicRepo.clearRequests();
            Build.Result r = lab.build("t4", both(lab, SHOP, NOT_SHOP), Lab.DEPENDENCIES).run();
            assertTrue(r.resolvedFrom("com.shop:pricing:1.4.2", "private"), r.toString());
            assertEquals(0, lab.publicRepo.requestsUnder("com/shop"));
        }
    }

    @Test
    void anUnpublishedInternalLibraryIsNeverAskedForInThePublicRepository() throws Exception {
        try (Lab lab = new Lab()) {
            lab.publicRepo.clearRequests();
            Build.Result filtered = lab.build("t5a", both(lab, SHOP, NOT_SHOP), "        cp 'com.shop:newlib:1.0'").run();
            assertFalse(filtered.ok());
            assertEquals(0, lab.publicRepo.requestsUnder("com/shop"));
            lab.build("t5b", both(lab, "", ""), "        cp 'com.shop:newlib:1.0'").run();
            assertTrue(lab.publicRepo.requestsUnder("com/shop") > 0);
        }
    }

    @Test
    void verificationMetadataRefusesAnUnlistedVersion() throws Exception {
        try (Lab lab = new Lab()) {
            Build build = lab.build("t6", both(lab, "", ""), Lab.DEPENDENCIES);
            assertTrue(build.run("--write-verification-metadata", "sha256").ok());
            assertTrue(Files.exists(build.dir().resolve("gradle").resolve("verification-metadata.xml")));
            assertTrue(build.run().ok());
            lab.publicRepo.publish("com.shop", "pricing", "99.0.0", "public (the attacker's)");
            Build.Result r = build.run();
            assertFalse(r.ok());
            assertTrue(r.failure().startsWith("not listed"));
        }
    }

    @Test
    void verificationMetadataRefusesSameVersionOtherBytes() throws Exception {
        try (Lab lab = new Lab()) {
            Build build = lab.build("t7", both(lab, "", ""), Lab.DEPENDENCIES);
            build.run("--write-verification-metadata", "sha256");
            lab.privateRepo.publish("com.shop", "pricing", "1.4.2", "private (replaced)");
            Build.Result r = build.run();
            assertFalse(r.ok());
            assertTrue(r.failure().startsWith("checksum differs"));
        }
    }

    @Test
    void aFilterForAnExactGroupDoesNotCoverItsSubgroups() throws Exception {
        try (Lab lab = new Lab()) {
            lab.publicRepo.publish("com.shop.tools", "signer", "9.0", "public (the attacker's)");
            String tools = "        cp 'com.shop.tools:signer:[1.0,)'";
            assertTrue(lab.build("t8a", both(lab, "                includeGroup 'com.shop'", "                excludeGroup 'com.shop'"), tools).run().resolvedFrom("com.shop.tools:signer:9.0", "public (the attacker's)"));
            assertTrue(lab.build("t8b", both(lab, SHOP, NOT_SHOP), tools).run().resolvedFrom("com.shop.tools:signer:1.0", "private"));
        }
    }

    @Test
    void checksumsWrittenFromACompromisedBuildTrustTheAttacker() throws Exception {
        try (Lab lab = new Lab()) {
            lab.publicRepo.publish("com.shop", "pricing", "99.0.0", "public (the attacker's)");
            Build build = lab.build("t9", both(lab, "", ""), Lab.DEPENDENCIES);
            build.run("--write-verification-metadata", "sha256");
            assertTrue(build.run().resolvedFrom("com.shop:pricing:99.0.0", "public (the attacker's)"));
        }
    }

    @Test
    void theRepositoryRemembersEveryRequest() throws Exception {
        try (Repo repo = new Repo("x")) {
            repo.publish("a.b", "c", "1", "x");
            assertTrue(repo.summary().isEmpty());
            assertEquals(0, repo.requestsUnder("a/b"));
        }
    }
}
