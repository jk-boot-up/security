package com.jk.explore.security.hostscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The rules over the output of the snapshot script, with no container. */
class ScannerTest {

    private static List<String> rules(String snapshot) {
        return Scanner.scan(Scanner.sections(snapshot)).stream().map(f -> f.rule() + " " + f.where()).toList();
    }

    @Test
    void theSnapshotIsSplitAtItsSectionLines() {
        Map<String, List<String>> s = Scanner.sections("#modes\n640 /etc/shadow\n#setuid\n\n#tcp\na b\n");
        assertEquals(List.of("640 /etc/shadow"), s.get("modes"));
        assertEquals(List.of(), s.get("setuid"));
        assertEquals(List.of("a b"), s.get("tcp"));
    }

    @Test
    void looseModesAreFoundAndCarryTheirFix() {
        List<Finding> findings = Scanner.scan(Scanner.sections("#modes\n644 /etc/shadow\n775 /etc/cron.daily/backup.sh\n600 /opt/app/config.properties\n"));
        assertEquals(2, findings.size());
        assertTrue(findings.stream().anyMatch(f -> f.fix().equals("chmod 640 /etc/shadow")));
        assertTrue(findings.stream().anyMatch(f -> f.fix().equals("chmod g-w /etc/cron.daily/backup.sh")));
    }

    @Test
    void aStickyWorldWritableDirectoryIsNotAFindingButAFileIs() {
        assertEquals(List.of(), rules("#writable\n1777 directory /var/tmp/shared\n"));
        assertEquals(List.of("world-writable-dir /var/www"), rules("#writable\n777 directory /var/www\n"));
        assertEquals(List.of("world-writable-file /var/www/a.txt"), rules("#writable\n666 regular file /var/www/a.txt\n").stream().map(s -> s.replace("regular file ", "")).toList());
    }

    @Test
    void accountsWithNoPasswordASecondRootAndDefaultNamesAreFound() {
        String snapshot = "#passwd\nroot:x:0:0::/root:/bin/sh\ntoor:x:0:0::/root:/bin/sh\ntest:x:1000:1000::/home/test:/bin/sh\napp:x:100:101::/home/app:/sbin/nologin\n#shadow\nroot:!::0:::::\ntoor:!::0:::::\ntest::19000:0:99999:7:::\napp:!::0:::::\n";
        assertEquals(List.of("empty-password test", "extra-root-account toor", "default-account test"), rules(snapshot));
    }

    @Test
    void sshdSettingsAreReadLineByLine() {
        String snapshot = "#sshd\nPermitRootLogin no\nPasswordAuthentication no\nPermitEmptyPasswords yes\nX11Forwarding yes\n";
        assertEquals(List.of("ssh-empty-passwords /etc/ssh/sshd_config", "ssh-x11 /etc/ssh/sshd_config"), rules(snapshot));
    }

    @Test
    void onlyListenersOnEveryInterfaceThatAreNotAllowedAreFound() {
        String snapshot = "#tcp\n  sl  local_address rem_address   st\n   0: 00000000:0017 00000000:0000 0A 00000000:00000000\n   1: 00000000:1F90 00000000:0000 0A 00000000:00000000\n"
                + "   2: 0100007F:0CEA 00000000:0000 0A 00000000:00000000\n   3: 00000000:0050 00000000:0000 01 00000000:00000000\n";
        assertEquals(List.of("open-port port 23"), rules(snapshot));
    }

    @Test
    void kernelSettingsAreCheckedAgainstTheirSafeValues() {
        assertEquals(List.of("accepts-redirects net.ipv4.conf.all.accept_redirects", "no-syncookies net.ipv4.tcp_syncookies"), rules("#sysctl\nnet/ipv4/conf/all/accept_redirects 1\nnet/ipv4/tcp_syncookies 0\n"));
        assertEquals(List.of(), rules("#sysctl\nnet/ipv4/conf/all/accept_redirects 0\nnet/ipv4/tcp_syncookies 1\n"));
    }
}
