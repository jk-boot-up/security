package com.jk.explore.security.patchingtrivy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormatsTest {

    private static String fixture(String name) throws Exception {
        return Files.readString(Path.of("fixtures", name));
    }

    @Test
    void versionsCompareAsNumbers() {
        assertTrue(Version.parse("1.10.0").compareTo(Version.parse("1.9.0")) > 0);
        assertEquals(0, Version.parse("1.9").compareTo(Version.parse("1.9.0")));
        assertTrue(Version.parse("2.0-beta9").isOlderThan(Version.parse("2.0")));
        assertTrue(Version.parse("2.14.1").isOlderThan(Version.parse("2.15.0")));
        assertEquals("2.3.1", Version.parse("2.3.1").toString());
        assertThrows(IllegalArgumentException.class, () -> Version.parse("1..2"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse(""));
    }

    @Test
    void dependencyNamesItselfTheWayScannersDo() {
        Dependency d = new Dependency("org.yaml", "snakeyaml", "1.26");
        assertEquals("pkg:maven/org.yaml/snakeyaml@1.26", d.purl());
        assertEquals("org.yaml:snakeyaml", d.name());
        assertEquals("org.yaml:snakeyaml:1.26", d.coordinates());
    }

    @Test
    void lockfileListsEveryResolvedLibrary() throws Exception {
        List<Dependency> locked = GradleLockfile.parse(fixture("gradle.lockfile"));
        assertEquals(5, locked.size());
        assertEquals("commons-codec:commons-codec:1.23.0", locked.get(0).coordinates());
        assertThrows(IllegalArgumentException.class, () -> GradleLockfile.parse("not-a-line=x"));
    }

    @Test
    void sbomRoundTripsAndMatchesTheCommittedFile() throws Exception {
        List<Dependency> locked = GradleLockfile.parse(fixture("gradle.lockfile"));
        String json = CycloneDx.write("online-store", "1.0.0", locked);
        assertEquals(locked, CycloneDx.read(json));
        assertEquals(fixture("online-store.cdx.json"), json);
        assertTrue(json.contains("\"bomFormat\": \"CycloneDX\"") && json.contains("\"specVersion\": \"1.6\""));
    }

    @Test
    void sbomChecksAreStrict() throws Exception {
        String json = fixture("online-store.cdx.json");
        assertThrows(IllegalArgumentException.class, () -> CycloneDx.read(json.replace("CycloneDX", "SPDX")));
        assertThrows(IllegalArgumentException.class, () -> CycloneDx.read(json.replace("\"1.6\"", "\"1.2\"")));
        assertThrows(IllegalArgumentException.class, () -> CycloneDx.read(json.replace("\"version\": \"1.26\"", "\"version\": \"1.31\"")));
        String duplicate = json.replace("\"bom-ref\": \"pkg:maven/org.yaml/snakeyaml@1.26\"", "\"bom-ref\": \"pkg:maven/commons-codec/commons-codec@1.23.0\"");
        assertThrows(IllegalArgumentException.class, () -> CycloneDx.read(duplicate));
    }

    @Test
    void sbomEscapesQuotes() {
        String json = CycloneDx.write("a\"b", "1", List.of());
        assertTrue(json.contains("a\\\"b"));
        assertEquals(List.of(), CycloneDx.read(json));
    }

    @Test
    void reportFixtureParses() throws Exception {
        String json = fixture("trivy-report.json");
        List<Vulnerability> found = TrivyReport.parse(json);
        assertEquals("0.76.0", TrivyReport.version(json));
        assertEquals(16, found.size());
        assertEquals(Map.of("CRITICAL", 3, "HIGH", 3, "MEDIUM", 10, "LOW", 0), Triage.countBySeverity(found));
        assertEquals(List.of(), TrivyReport.parse("{\"Results\": []}"));
        assertEquals(List.of(), TrivyReport.parse("{}"));
    }

    @Test
    void nearestFixIsTheSmallestVersionAboveTheInstalledOne() {
        Vulnerability log4shell = new Vulnerability("CVE-2021-44228", "log4j-core", "2.14.1", "2.15.0, 2.3.1, 2.12.2", "CRITICAL", "");
        assertEquals("2.15.0", log4shell.nearestFix().orElseThrow().toString());
        Vulnerability old = new Vulnerability("X", "p", "2.3.0", "2.15.0, 2.3.1, 2.12.2", "HIGH", "");
        assertEquals("2.3.1", old.nearestFix().orElseThrow().toString());
        assertTrue(new Vulnerability("X", "p", "1.0.0", "", "LOW", "").nearestFix().isEmpty());
        assertTrue(new Vulnerability("X", "p", "3.0.0", "2.0.0", "LOW", "").nearestFix().isEmpty());
    }

    @Test
    void triageOrdersBySeverityAndPicksOneTargetPerLibrary() throws Exception {
        List<Vulnerability> found = TrivyReport.parse(fixture("trivy-report.json"));
        List<Vulnerability> ordered = Triage.ordered(found);
        assertEquals("CRITICAL", ordered.get(0).severity());
        assertEquals("MEDIUM", ordered.get(ordered.size() - 1).severity());
        assertEquals(ordered, Triage.ordered(ordered));
        Map<String, java.util.Optional<Version>> targets = Triage.upgradeTargets(found);
        assertEquals("2.25.4", targets.get("org.apache.logging.log4j:log4j-core").orElseThrow().toString());
        assertEquals("2.0", targets.get("org.yaml:snakeyaml").orElseThrow().toString());
        assertEquals("1.10.0", targets.get("org.apache.commons:commons-text").orElseThrow().toString());
    }

    @Test
    void aLibraryWithNoFixHasNoTarget() {
        Map<String, java.util.Optional<Version>> targets = Triage.upgradeTargets(List.of(new Vulnerability("X", "p", "1.0.0", "", "HIGH", "")));
        assertTrue(targets.get("p").isEmpty());
    }

    @Test
    void ownRangeChecksAgreeWithTheKnownAdvisories() {
        OwnAdvisory log4shell = OwnAdvisory.known().get(0);
        assertTrue(log4shell.affects("org.apache.logging.log4j:log4j-core", "2.14.1"));
        assertTrue(!log4shell.affects("org.apache.logging.log4j:log4j-core", "2.15.0"));
        assertTrue(!log4shell.affects("org.apache.logging.log4j:log4j-core", "2.0-alpha1"));
        assertTrue(!log4shell.affects("other:thing", "2.14.1"));
    }

    @Test
    void verificationMetadataIsReadAndComparedByHash() throws Exception {
        String xml = fixture("verified-build/gradle/verification-metadata.xml");
        Map<String, String> sums = VerificationMetadata.parse(xml);
        assertEquals(6, sums.size());
        assertEquals("47504a3ce71d73a1b0af07675546b8d8c5af87f82ef12b6381f9eb3c17dd5a5b", sums.get("commons-codec:commons-codec:1.23.0/commons-codec-1.23.0.jar"));
        Map<String, String> changed = VerificationMetadata.parse(xml.replace("47504a3ce7", "0000000000"));
        assertEquals(List.of("commons-codec:commons-codec:1.23.0/commons-codec-1.23.0.jar"), VerificationMetadata.differences(sums, changed));
        assertEquals(List.of(), VerificationMetadata.differences(sums, sums));
    }

    @Test
    void verificationMetadataRefusesADoctype() {
        String evil = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><verification-metadata/>";
        assertThrows(IllegalArgumentException.class, () -> VerificationMetadata.parse(evil));
    }
}
