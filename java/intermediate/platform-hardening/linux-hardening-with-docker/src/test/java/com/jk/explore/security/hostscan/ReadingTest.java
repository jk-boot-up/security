package com.jk.explore.security.hostscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The readers over the script's output, with no container: listeners in hex, accounts, and the report line. */
class ReadingTest {

    @Test
    void listenersAreReadFromHexAndSortedByPort() {
        List<Listeners.Listener> l = Listeners.read(List.of(
                "  sl  local_address rem_address   st",
                "   0: 00000000000000000000000000000000:18EB 00000000000000000000000000000000:0000 0A 0",
                "   1: 0100007F:0CEA 00000000:0000 0A 0",
                "   2: 00000000:0017 00000000:0000 0A 0",
                "   3: 00000000:0050 00000000:0000 01 0"));
        assertEquals(List.of(23, 3306, 6379), l.stream().map(Listeners.Listener::port).toList());
        assertEquals("0.0.0.0", l.get(0).address());
        assertEquals("127.0.0.1", l.get(1).address());
        assertFalse(l.get(1).everywhere());
        assertEquals("::", l.get(2).address());
        assertTrue(l.get(2).everywhere());
    }

    @Test
    void theSnapshotNamesTheSecondRootAndTheEmptyPassword() {
        Snapshot s = Snapshot.of("#passwd\nroot:x:0:0::/root:/bin/sh\ntoor:x:0:0::/root:/bin/sh\ntest:x:1000:1000::/home/test:/bin/sh\n"
                + "app:x:100:101::/home/app:/sbin/nologin\n#shadow\nroot:!::0:::::\ntoor:!::0:::::\ntest::19000:0:99999:7:::\napp:::0:::::\n");
        assertEquals(List.of("root", "toor"), s.uidZero());
        assertEquals(List.of("test"), s.emptyPasswords(), "app has no login shell");
    }

    @Test
    void aReportLineMarksTheFindingsAMachineCanFix() {
        assertEquals("HIGH     loose-permissions   /etc/shadow  auto", new Finding(Finding.Severity.HIGH, "loose-permissions", "/etc/shadow", "m", "chmod 640 /etc/shadow").toString());
        assertEquals("LOW      ssh-x11             /etc/ssh/sshd_config", new Finding(Finding.Severity.LOW, "ssh-x11", "/etc/ssh/sshd_config", "m", null).toString());
    }

    @Test
    void theFindingsAreGroupedByThePartOfTheHostTheyAreAbout() {
        assertEquals("accounts", HostScanDemo.group(new Finding(Finding.Severity.HIGH, "default-account", "test", "m", null)));
        assertEquals("sshd", HostScanDemo.group(new Finding(Finding.Severity.LOW, "ssh-x11", "f", "m", null)));
        assertEquals("ports", HostScanDemo.group(new Finding(Finding.Severity.CRITICAL, "open-port", "port 23", "m", null)));
    }
}
