package com.jk.explore.security.sbomscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Parsing a CycloneDX document needs no syft and no container, so these run under the default test task. */
class SbomParseTest {

    private static final String JSON = """
            {"specVersion":"1.6",
             "metadata":{"component":{"name":"shop-app","version":"1.0.0"}},
             "components":[
               {"type":"library","name":"log4j-core","purl":"pkg:maven/org.apache.logging.log4j/log4j-core@2.17.1"},
               {"type":"file","name":"/app/lib/log4j-core-2.17.1.jar","hashes":[{"alg":"SHA-1","content":"abc123"}]},
               {"type":"library","name":"musl","purl":"pkg:apk/alpine/musl@1.2.5-r9"},
               {"type":"library","name":"guava","purl":"pkg:maven/com.google.guava/guava@32.0.0"},
               {"type":"application","name":"shop-app"}
             ]}
            """;

    @Test
    void readsOnlyLibraryComponentsThatHaveaPackageUrl() {
        Sbom sbom = Sbom.parse(JSON);
        assertEquals(3, sbom.libraries().size(), "the application component has no purl and is not a library");
        assertTrue(sbom.libraries().first().contains("musl"), "the set is ordered: pkg:apk before pkg:maven");
    }

    @Test
    void findsLibrariesByNameAndReadsTheMetadata() {
        Sbom sbom = Sbom.parse(JSON);
        assertEquals(List.of("pkg:maven/org.apache.logging.log4j/log4j-core@2.17.1"), sbom.withName("log4j"));
        assertEquals("1.6", sbom.specVersion());
        assertEquals("shop-app:1.0.0", sbom.subject());
    }

    @Test
    void mentionsSearchesTheRawText() {
        Sbom sbom = Sbom.parse(JSON);
        assertTrue(sbom.mentions("log4j-core"));
        assertFalse(sbom.mentions("struts"));
    }

    @Test
    void readsTheHashOfOneLibraryAndTheOperatingSystemPackages() {
        Sbom sbom = Sbom.parse(JSON);
        assertEquals("abc123", sbom.fileHash("/app/lib/log4j-core-2.17.1.jar", "SHA-1"));
        assertEquals("", sbom.fileHash("/app/lib/log4j-core-2.17.1.jar", "SHA-256"));
        assertEquals(List.of("pkg:apk/alpine/musl@1.2.5-r9"), sbom.osPackages());
    }
}
