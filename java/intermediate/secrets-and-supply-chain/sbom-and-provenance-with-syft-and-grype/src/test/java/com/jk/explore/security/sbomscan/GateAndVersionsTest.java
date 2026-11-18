package com.jk.explore.security.sbomscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The gate, the version order and the package URL parts need no Docker, syft or grype, so these run under the default test task. */
class GateAndVersionsTest {

    private static Scan.Finding finding(String id, String alias, String severity, String... fixes) {
        return new Scan.Finding("log4j-core", "2.14.1", id, alias, severity, List.of(fixes));
    }

    /** The shape grype reports for log4j-core 2.14.1: GitHub ids with CVE aliases, a critical twice for core and api. */
    private static final Scan OLD = new Scan(List.of(
            finding("GHSA-jfh8-c2jp-5v3q", "CVE-2021-44228", "Critical", "2.15.0", "2.12.2"),
            finding("GHSA-7rjr-3q55-vv33", "CVE-2021-45046", "Critical", "2.16.0"),
            finding("GHSA-p6xc-xr62-6r2g", "CVE-2021-45105", "High", "2.17.0"),
            finding("GHSA-8489-44mv-ggj8", "CVE-2021-44832", "Medium", "2.17.1"),
            new Scan.Finding("libcrypto3", "3.3.0", "CVE-2099-0001", "", "High", List.of("3.3.1"))));

    private static final Scan PATCHED = new Scan(List.of(finding("GHSA-p6xc-xr62-6r2g", "CVE-2021-45105", "High", "2.17.0")));

    @Test
    void theGateCountsOnlyCriticalAndHighAdvisoriesInTheStoresLibraries() {
        assertEquals("refused: 2 critical, 1 high", Gate.check(OLD, List.of(), 100));
        assertEquals("deployed", Gate.check(new Scan(List.of()), List.of(), 100));
    }

    @Test
    void anExceptionCoversAHighAdvisoryUntilItsEndDayAndNeverACriticalOne() {
        List<Gate.Waiver> waivers = List.of(new Gate.Waiver("CVE-2021-45105", "reviewed", 120), new Gate.Waiver("CVE-2021-44228", "no", 120));
        assertEquals("refused: 2 critical, 0 high", Gate.check(OLD, waivers, 100));
        assertEquals("deployed, 1 high excepted", Gate.check(PATCHED, waivers, 120));
        assertEquals("refused: 0 critical, 1 high", Gate.check(PATCHED, waivers, 121));
    }

    @Test
    void eachAdvisoryGetsTheFirstFixAfterTheInstalledVersionWorstFirst() {
        List<SbomScanDemo.Advisory> a = SbomScanDemo.advisories(OLD);
        assertEquals(List.of("CVE-2021-44228", "CVE-2021-45046", "CVE-2021-45105"), a.stream().map(SbomScanDemo.Advisory::cve).toList());
        assertEquals(List.of("2.15.0", "2.16.0", "2.17.0"), a.stream().map(SbomScanDemo.Advisory::fixedIn).toList(), "2.12.2 is a backport older than 2.14.1");
    }

    @Test
    void versionsCompareAsNumbers() {
        assertTrue(Versions.compare("2.9.0", "2.17.0") < 0);
        assertTrue(Versions.compare("2.14.1", "2.15.0") < 0);
        assertEquals(0, Versions.compare("2.15", "2.15.0"));
    }

    @Test
    void aPackageUrlSplitsIntoItsParts() {
        Purl p = Purl.parse("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1");
        assertEquals(new Purl("maven", "org.apache.logging.log4j", "log4j-core", "2.14.1"), p);
        assertEquals("log4j-core@2.14.1", p.shortName());
        assertEquals(new Purl("apk", "alpine", "musl", "1.2.5-r9"), Purl.parse("pkg:apk/alpine/musl@1.2.5-r9?arch=aarch64&distro=alpine-3.22"));
        assertThrows(IllegalArgumentException.class, () -> Purl.parse("log4j-core"));
    }
}
