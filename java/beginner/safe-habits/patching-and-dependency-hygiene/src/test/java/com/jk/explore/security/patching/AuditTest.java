package com.jk.explore.security.patching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditTest {

    private static Sbom sbom() throws IOException {
        return Sbom.parse(PatchingDemo.resource("sbom.json"));
    }

    private static List<Advisory> advisories() throws IOException {
        return Advisory.parse(PatchingDemo.resource("advisories.json"));
    }

    @Test
    void inventoryShapeIsRead() throws IOException {
        Sbom sbom = sbom();
        assertEquals("shop-app@1.0.0", sbom.root());
        assertEquals(7, sbom.components().size());
        assertEquals(List.of("acme-http@2.3.1", "acme-image@0.9.2", "acme-log@4.2.0"), sbom.direct());
        assertEquals(6, sbom.libraries().size());
    }

    @Test
    void shortestPathsAreFound() throws IOException {
        Sbom sbom = sbom();
        assertEquals(List.of("shop-app@1.0.0", "acme-http@2.3.1", "acme-json@1.8.2"), sbom.pathTo("acme-json@1.8.2").orElseThrow());
        assertEquals(List.of("shop-app@1.0.0"), sbom.pathTo("shop-app@1.0.0").orElseThrow());
        assertTrue(sbom.pathTo("nothing@1.0.0").isEmpty());
    }

    @Test
    void treeIsIndented() throws IOException {
        List<String> tree = sbom().tree();
        assertEquals("shop-app@1.0.0", tree.get(0));
        assertEquals("  acme-http@2.3.1", tree.get(1));
        assertEquals("    acme-json@1.8.2", tree.get(2));
        assertEquals(7, tree.size());
    }

    @Test
    void aSharedDependencyIsFoundOnTheShorterRoute() {
        Sbom sbom = Sbom.parse("""
                {"metadata":{"component":"a@1.0.0","usedFeatures":[]},"components":[
                 {"name":"a","version":"1.0.0","dependsOn":["b@1.0.0","c@1.0.0"]},
                 {"name":"b","version":"1.0.0","dependsOn":["d@1.0.0"]},
                 {"name":"c","version":"1.0.0","dependsOn":["d@1.0.0"]},
                 {"name":"d","version":"1.0.0","dependsOn":[]}]}""");
        assertEquals(List.of("a@1.0.0", "b@1.0.0", "d@1.0.0"), sbom.pathTo("d@1.0.0").orElseThrow());
        assertEquals(3, sbom.libraries().size());
    }

    @Test
    void cyclesDoNotLoopForever() {
        Sbom sbom = Sbom.parse("""
                {"metadata":{"component":"a@1.0.0","usedFeatures":[]},"components":[
                 {"name":"a","version":"1.0.0","dependsOn":["b@1.0.0"]},
                 {"name":"b","version":"1.0.0","dependsOn":["a@1.0.0"]}]}""");
        assertEquals(2, sbom.tree().size());
        assertEquals(2, sbom.libraries().size());
    }

    @Test
    void brokenInventoriesAreRefused() {
        String head = "{\"metadata\":{\"component\":\"a@1.0.0\",\"usedFeatures\":[]},\"components\":[";
        assertThrows(IllegalArgumentException.class, () -> Sbom.parse(head
                + "{\"name\":\"a\",\"version\":\"1.0.0\",\"dependsOn\":[\"zzz@1.0.0\"]}]}"));
        assertThrows(IllegalArgumentException.class, () -> Sbom.parse(head
                + "{\"name\":\"a\",\"version\":\"1.0.0\",\"dependsOn\":[]},{\"name\":\"a\",\"version\":\"1.0.0\",\"dependsOn\":[]}]}"));
        assertThrows(IllegalArgumentException.class, () -> Sbom.parse(head
                + "{\"name\":\"b\",\"version\":\"1.0.0\",\"dependsOn\":[]}]}"));
        assertThrows(IllegalArgumentException.class, () -> Sbom.parse(head
                + "{\"name\":\"a\",\"version\":\"one\",\"dependsOn\":[]}]}"));
    }

    @Test
    void threeOfFiveAdvisoriesApply() throws IOException {
        List<Finding> findings = Audit.scan(sbom(), advisories());
        assertEquals(List.of("DEMO-2026-0001", "DEMO-2026-0002", "DEMO-2026-0003"), findings.stream().map(f -> f.advisory().id()).toList());
    }

    @Test
    void theFixedVersionIsNotFlagged() throws IOException {
        List<Finding> findings = Audit.scan(sbom(), advisories());
        assertFalse(findings.stream().anyMatch(f -> f.advisory().component().equals("acme-http")));
        assertFalse(findings.stream().anyMatch(f -> f.advisory().component().equals("acme-crypto")));
    }

    @Test
    void transitiveAndDirectAdviceDiffer() throws IOException {
        List<Finding> findings = Audit.scan(sbom(), advisories());
        assertFalse(findings.get(0).direct());
        assertTrue(findings.get(0).advice().startsWith("transitive: add a constraint forcing acme-json to 1.8.3"));
        assertTrue(findings.get(2).direct());
        assertEquals("direct: change acme-log from 4.2.0 to 4.2.1", findings.get(2).advice());
    }

    @Test
    void reachabilityComesFromTheFeaturesTheProgramUses() throws IOException {
        List<Finding> findings = Audit.scan(sbom(), advisories());
        assertTrue(findings.get(0).reachable());
        assertFalse(findings.get(1).reachable());
        assertTrue(findings.get(2).reachable());
    }

    @Test
    void triagePutsReachableFirstThenTheHigherScore() throws IOException {
        List<Finding> order = Audit.triage(Audit.scan(sbom(), advisories()));
        assertEquals(List.of("DEMO-2026-0001", "DEMO-2026-0003", "DEMO-2026-0002"), order.stream().map(f -> f.advisory().id()).toList());
        assertEquals("fix now", order.get(0).priority());
        assertEquals("fix inside the patch window", order.get(1).priority());
        assertEquals("schedule with the next upgrade", order.get(2).priority());
    }

    @Test
    void anAdvisoryWithABadVectorIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Advisory.parse("""
                {"advisories":[{"id":"X","component":"c","affected":"<1.0.0","fixed":"1.0.0","vector":"nonsense","feature":"f","summary":"s"}]}"""));
    }
}
