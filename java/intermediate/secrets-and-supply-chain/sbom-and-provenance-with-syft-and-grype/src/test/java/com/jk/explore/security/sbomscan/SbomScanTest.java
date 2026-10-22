package com.jk.explore.security.sbomscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Real images built here, catalogued by the real syft and scanned by the real grype. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SbomScanTest {

    private Lab lab;
    private String old;
    private String patched;
    private String current;
    private String renamed;
    private Sbom oldSbom;
    private Scan oldScan;
    private Scan patchedScan;
    private Scan currentScan;

    @BeforeAll
    void start() throws Exception {
        lab = new Lab();
        old = lab.build(Lab.OLD, Lab.OLD, null);
        patched = lab.build("2.16.0", "2.16.0", null);
        current = lab.build(Lab.NEW, Lab.NEW, null);
        renamed = lab.build("renamed", Lab.OLD, "blob.dat");
        oldSbom = Sbom.of(old);
        oldScan = Scan.ofImage(old);
        patchedScan = Scan.ofImage(patched);
        currentScan = Scan.ofImage(current);
    }

    private static Sbom sbomOf(String image) throws Exception {
        return Sbom.of(image);
    }

    @Test
    void theBillListsLog4jWithItsVersionAsAPackageUrl() {
        assertFalse(oldSbom.withName("log4j-core").isEmpty());
        assertTrue(oldSbom.withName("log4j-core").stream().anyMatch(p -> p.contains("2.14.1")));
        assertTrue(oldSbom.specVersion().startsWith("1."));
    }

    @Test
    void theBillIsAboutTheImageItWasWrittenFor() {
        assertTrue(oldSbom.subject().contains("shop-sbom"));
        assertTrue(oldSbom.subject().contains(Lab.OLD) || oldSbom.mentions(Lab.OLD));
    }

    @Test
    void log4jShellIsFoundInTheOldImage() {
        assertTrue(oldScan.ids("log4j").stream().anyMatch(id -> id.contains("44228")) || oldScan.inPackages("log4j").stream().anyMatch(f -> f.alias().equals("CVE-2021-44228")));
    }

    @Test
    void theCriticalAdvisoriesAreFixedByTheNextReleases() {
        assertTrue(oldScan.inPackages("log4j").stream().filter(f -> f.severity().equals("Critical")).anyMatch(f -> f.fixedIn().contains("2.15.0") || f.fixedIn().contains("2.16.0")));
    }

    @Test
    void anImageScanAndABillScanAgree() throws Exception {
        Path file = Files.writeString(lab.work().resolve("old.cdx.json"), oldSbom.json());
        assertEquals(oldScan.ids("log4j"), Scan.ofSbom(file).ids("log4j"));
    }

    @Test
    void theCurrentReleaseHasNoSevereAdvisoryInLog4j() {
        assertTrue(currentScan.inPackages("log4j").stream().noneMatch(Scan.Finding::severe));
    }

    @Test
    void theGateRefusesTheOldImageAndAnExceptionCannotCoverACriticalOne() {
        assertTrue(SbomScanDemo.gate(oldScan, List.of(), 100).startsWith("refused"));
        var waivers = oldScan.inPackages("log4j").stream().map(f -> new SbomScanDemo.Waiver(f.id(), "none", 500)).toList();
        assertTrue(SbomScanDemo.gate(oldScan, waivers, 100).startsWith("refused"));
    }

    @Test
    void anExceptionForAHighAdvisoryWorksUntilItRunsOut() {
        var waivers = patchedScan.inPackages("log4j").stream().filter(f -> f.severity().equals("High")).map(f -> new SbomScanDemo.Waiver(f.id(), "reviewed", 120)).toList();
        assertTrue(SbomScanDemo.gate(patchedScan, List.of(), 100).startsWith("refused"));
        assertTrue(SbomScanDemo.gate(patchedScan, waivers, 100).startsWith("deployed"));
        assertTrue(SbomScanDemo.gate(patchedScan, waivers, 121).startsWith("refused"));
    }

    @Test
    void theCurrentImagePassesTheGate() {
        assertEquals("deployed", SbomScanDemo.gate(currentScan, List.of(), 130));
    }

    @Test
    void aRenamedJarIsNotRecognisedByTheCatalogue() throws Exception {
        assertTrue(sbomOf(renamed).withName("log4j-core").isEmpty());
        assertTrue(Scan.ofImage(renamed).inPackages("log4j-core").isEmpty());
    }

    @Test
    void aBillOfTheOldImageDoesNotDescribeTheNewOne() throws Exception {
        assertFalse(sbomOf(current).libraries().equals(oldSbom.libraries()));
        assertFalse(oldSbom.mentions("log4j-core@" + Lab.NEW) || oldSbom.mentions("log4j-core-" + Lab.NEW));
    }
}
