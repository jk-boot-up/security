package com.jk.explore.security.provenance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SbomAndBuildTest {

    @Test
    void theSbomIsCycloneDxWithPurlsAndHashes() throws Exception {
        byte[] jar = Archive.reproducible(ProvenanceDemo.files("1.4.0"));
        String sbom = Sbom.write("shop", "1.4.0", jar, ProvenanceDemo.libraries());
        Object root = Json.read(sbom);
        assertEquals("CycloneDX", Json.text(root, "bomFormat"));
        assertEquals("1.6", Json.text(root, "specVersion"));
        assertEquals(List.of("pkg:maven/com.acme/acme-http@3.0.2", "pkg:maven/com.acme/acme-json@2.4.1", "pkg:maven/com.acme/acme-log@1.9.0"),
                Sbom.purls(sbom));
        assertTrue(Sbom.describes(sbom, jar));
        assertFalse(Sbom.describes(sbom, Archive.reproducible(ProvenanceDemo.files("1.4.1"))));
        assertEquals(List.of(), Sbom.changedComponents(sbom, ProvenanceDemo.libraries()));
    }

    @Test
    void reproducibleBuildsDoNotDependOnOrderOrClock() {
        Map<String, byte[]> files = ProvenanceDemo.files("1.4.0");
        assertArrayEquals(Archive.reproducible(files), Archive.reproducible(new java.util.TreeMap<>(files).descendingMap()));
        List<String> order = List.copyOf(files.keySet());
        byte[] early = Archive.careless(order, files, LocalDateTime.of(2026, 1, 1, 0, 0));
        byte[] late = Archive.careless(order, files, LocalDateTime.of(2026, 1, 2, 0, 0));
        assertNotEquals(Digests.hex(Digests.sha256(early)), Digests.hex(Digests.sha256(late)));
    }

    @Test
    void slsaLevelsFollowTheFactsTheyNeed() {
        assertEquals(0, new Pipeline("a", false, true, true).level());
        assertEquals(1, new Pipeline("b", true, false, true).level());
        assertEquals(2, new Pipeline("c", true, true, false).level());
        assertEquals(3, new Pipeline("d", true, true, true).level());
        assertTrue(new Pipeline("d", true, true, true).missing().isEmpty());
    }
}
