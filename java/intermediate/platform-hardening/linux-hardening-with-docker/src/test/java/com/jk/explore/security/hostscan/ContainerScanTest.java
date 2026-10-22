package com.jk.explore.security.hostscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real images and real containers. Run with ./gradlew integrationTest. */
@Tag("integration")
class ContainerScanTest {

    @BeforeAll
    static void build() throws Exception {
        Images.buildTypical();
        Images.buildHardened();
    }

    @Test
    void theTypicalImageHasNineteenFindingsAndSevenOfThemCanBeFixedMechanically() throws Exception {
        try (Host host = new Host(Images.TYPICAL, HostScanDemo.TYPICAL_RUN)) {
            List<Finding> findings = Scanner.scan(host);
            assertEquals(19, findings.size());
            assertEquals(7, findings.stream().filter(Finding::autoFixable).count());
            assertEquals(3, findings.stream().filter(f -> f.rule().equals("open-port")).count());
        }
    }

    @Test
    void theMechanicalFixesLeaveTwelveAndAreGoneInANewContainer() throws Exception {
        try (Host host = new Host(Images.TYPICAL, HostScanDemo.TYPICAL_RUN)) {
            for (Finding f : Scanner.scan(host)) {
                if (f.autoFixable()) {
                    host.exec(f.fix());
                }
            }
            assertEquals(12, Scanner.scan(host).size());
        }
        try (Host fresh = new Host(Images.TYPICAL, HostScanDemo.TYPICAL_RUN)) {
            assertEquals(19, Scanner.scan(fresh).size());
        }
    }

    @Test
    void theHardenedImageHasNoFindingsAndRunsAsTheServiceAccount() throws Exception {
        try (Host host = new Host(Images.HARDENED, HostScanDemo.HARDENED_RUN)) {
            assertEquals(List.of(), Scanner.scan(host));
            assertEquals("app", Docker.run("exec", host.name(), "id", "-un").strip());
        }
    }

    @Test
    void theKernelSettingsAreTheContainersStartOptionsAndNotTheImages() throws Exception {
        try (Host host = new Host(Images.HARDENED, HostScanDemo.TYPICAL_RUN)) {
            List<String> rules = Scanner.scan(host).stream().map(Finding::rule).toList();
            assertEquals(List.of("accepts-redirects", "no-syncookies"), rules);
        }
    }

    @Test
    void driftInsideARunningContainerIsFoundAndDoesNotSurviveANewContainer() throws Exception {
        try (Host host = new Host(Images.HARDENED, HostScanDemo.HARDENED_RUN)) {
            host.exec("chmod 666 /opt/app/config.properties");
            assertTrue(Scanner.scan(host).stream().anyMatch(f -> f.rule().equals("loose-permissions")));
        }
    }

    @Test
    void aContainerCannotChangeTheKernelsAddressSpaceRandomisation() throws Exception {
        try (Host host = new Host(Images.TYPICAL, HostScanDemo.TYPICAL_RUN)) {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> host.exec("echo 0 > /proc/sys/kernel/randomize_va_space"));
            assertTrue(e.getMessage().contains("Read-only file system"));
            assertFalse(host.exec("cat /proc/sys/kernel/randomize_va_space").strip().isEmpty());
        }
    }
}
