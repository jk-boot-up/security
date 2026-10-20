package com.jk.explore.security.linuxhard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. It works in a temporary directory whose name it never prints, and needs Unix file modes. */
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("unix"), "needs Unix file modes");
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), LinuxHardeningDemo.run());
    }
}
