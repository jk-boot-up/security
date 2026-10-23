package com.jk.explore.security.securepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipelinePartsTest {

    @TempDir
    Path dir;

    @Test
    void sha256OfAbcIsTheKnownValue() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Digest.sha256("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void theSbomListsWhatShipsAndNotTheTestLibraries() {
        String gradle = """
                dependencies {
                    implementation 'commons-codec:commons-codec:1.17.1'
                    api "ignored:double-quotes:1"
                    runtimeOnly 'org.example:runtime:2.0'
                    testImplementation 'org.junit.jupiter:junit-jupiter'
                }
                """;
        List<Sbom.Component> found = Sbom.components(gradle);
        assertEquals(2, found.size());
        assertEquals("pkg:maven/commons-codec/commons-codec@1.17.1", found.get(0).purl());
        String bom = Sbom.cycloneDx(found);
        assertTrue(bom.contains("\"bomFormat\":\"CycloneDX\""));
        assertTrue(bom.contains("pkg:maven/org.example/runtime@2.0"));
        assertFalse(bom.contains("junit"));
    }

    @Test
    void provenanceRecordsTheSubjectTheBuilderAndTheSource() throws Exception {
        String json = Provenance.create(new Provenance.Facts("x.jar", "ab".repeat(32), "c".repeat(40), "d".repeat(40),
                List.of(new Sbom.Component("g", "a", "1")), "ef".repeat(32)));
        Path file = dir.resolve("p.json");
        Files.writeString(file, json);
        var p = Provenance.read(file);
        assertEquals("ab".repeat(32), Provenance.subjectDigest(p));
        assertEquals(Provenance.BUILDER, Provenance.builderId(p));
        assertEquals("c".repeat(40), Provenance.sourceCommit(p));
        assertEquals("d".repeat(40), Provenance.sourceTree(p));
        assertEquals(Provenance.COMMAND, Provenance.command(p));
        assertEquals(3, Provenance.inputs(p));
    }

    @Test
    void aGateThatCannotRunIsAnError() {
        assertEquals(Gate.Outcome.ERROR, Gate.tool(dir, List.of("no-such-tool-" + "anywhere"), 1));
    }

    @Test
    void exitCodesBecomePassFailOrError() {
        assertEquals(Gate.Outcome.PASS, Gate.tool(dir, List.of("sh", "-c", "exit 0"), 7));
        assertEquals(Gate.Outcome.FAIL, Gate.tool(dir, List.of("sh", "-c", "exit 7"), 7));
        assertEquals(Gate.Outcome.ERROR, Gate.tool(dir, List.of("sh", "-c", "exit 1"), 7));
    }

    @Test
    void onlyAPipelineThatFailsOpenGoesOnAfterAnError() {
        assertFalse(Gate.goesOn(Gate.Outcome.ERROR, true));
        assertTrue(Gate.goesOn(Gate.Outcome.ERROR, false));
        assertFalse(Gate.goesOn(Gate.Outcome.FAIL, false));
        assertTrue(Gate.goesOn(Gate.Outcome.PASS, true));
    }

    private Path jar(String name, long time, String content) throws Exception {
        Path p = dir.resolve(name);
        try (OutputStream o = Files.newOutputStream(p); ZipOutputStream z = new ZipOutputStream(o)) {
            ZipEntry e = new ZipEntry("a.txt");
            e.setTime(time);
            z.putNextEntry(e);
            z.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            z.closeEntry();
        }
        return p;
    }

    @Test
    void theComparisonSeesTimesAndContent() throws Exception {
        Path a = jar("a.jar", 1_700_000_000_000L, "same");
        Reproducibility.Comparison same = Reproducibility.compare(a, jar("b.jar", 1_700_000_000_000L, "same"));
        assertTrue(same.identical());
        Reproducibility.Comparison times = Reproducibility.compare(a, jar("c.jar", 1_800_000_000_000L, "same"));
        assertFalse(times.identical());
        assertTrue(times.differentTimes());
        assertTrue(times.differentContent().isEmpty());
        Reproducibility.Comparison content = Reproducibility.compare(a, jar("d.jar", 1_700_000_000_000L, "other"));
        assertEquals(List.of("a.txt"), content.differentContent());
        assertNotEquals(content.first(), content.second());
    }

    @Test
    void theLibraryBuildsDifferOnlyInTheArchiveSettings() {
        String reproducible = BankLib.buildGradle(BankLib.Flavour.CLEAN);
        assertTrue(reproducible.contains("preserveFileTimestamps = false"));
        assertTrue(reproducible.contains("reproducibleFileOrder = true"));
        assertTrue(BankLib.buildGradle(BankLib.Flavour.FILE_TIMES).contains("preserveFileTimestamps = true"));
        assertTrue(BankLib.buildGradle(BankLib.Flavour.BUILD_PATH).contains("projectDir"));
        assertTrue(Sbom.components(BankLib.buildGradle(BankLib.Flavour.VULNERABLE_DEPENDENCY)).size() > Sbom.components(reproducible).size());
    }
}
