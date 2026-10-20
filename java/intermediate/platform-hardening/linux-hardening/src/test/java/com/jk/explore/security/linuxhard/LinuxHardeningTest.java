package com.jk.explore.security.linuxhard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinuxHardeningTest {

    private Fixture host;
    private Scanner scanner;

    @BeforeEach
    void setUp() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("unix"), "needs Unix file modes");
        host = Fixture.insecure();
        scanner = new Scanner(host.root(), Policy.standard());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (host != null) {
            host.close();
        }
    }

    private boolean has(List<Finding> f, String rule, String subject) {
        return f.stream().anyMatch(x -> x.rule().equals(rule) && x.subject().equals(subject));
    }

    @Test
    void thePreparedHostHasTwentyFindings() {
        List<Finding> f = scanner.scan();
        assertEquals(20, f.size());
        assertEquals(7, f.stream().filter(x -> x.severity() == Severity.CRITICAL).count());
    }

    @Test
    void findingsAreSortedWorstFirst() {
        List<Finding> f = scanner.scan();
        for (int i = 1; i < f.size(); i++) {
            assertTrue(f.get(i - 1).severity().compareTo(f.get(i).severity()) >= 0);
        }
    }

    @Test
    void theAllowListedSetuidToolsAndTheStickyTmpAreNotFlagged() {
        List<Finding> f = scanner.scan();
        assertFalse(has(f, "unexpected-setuid", "/usr/bin/passwd"));
        assertFalse(has(f, "unexpected-setuid", "/usr/bin/sudo"));
        assertFalse(f.stream().anyMatch(x -> x.subject().equals("/tmp")));
        assertTrue(has(f, "unexpected-setuid", "/tmp/.hidden-shell"));
    }

    @Test
    void worldWritableFilesAndDirectoriesWithoutTheStickyBitAreFlagged() {
        List<Finding> f = scanner.scan();
        assertTrue(has(f, "world-writable-dir", "/var/www/uploads"));
        assertTrue(has(f, "world-writable-file", "/var/www/uploads/readme.txt"));
    }

    @Test
    void secretsAndKeysMustBeTighterThanTheyAre() {
        List<Finding> f = scanner.scan();
        assertTrue(has(f, "loose-permissions", "/etc/shadow"));
        assertTrue(has(f, "loose-permissions", "/etc/ssh/ssh_host_ed25519_key"));
        assertTrue(has(f, "loose-permissions", "/opt/app/config.properties"));
        assertFalse(has(f, "loose-permissions", "/etc/passwd"), "0644 is allowed for passwd");
    }

    @Test
    void cronScriptsThatAGroupCanWriteAreFlagged() {
        assertTrue(has(scanner.scan(), "cron-group-writable", "/etc/cron.daily/backup.sh"));
    }

    @Test
    void accountProblemsAreFound() {
        List<Finding> f = scanner.scan();
        assertTrue(has(f, "extra-root-account", "toor"));
        assertTrue(has(f, "empty-password", "test"));
        assertTrue(has(f, "default-account", "test"));
        assertFalse(has(f, "default-account", "ana"));
        assertFalse(has(f, "extra-root-account", "root"));
        assertFalse(f.stream().anyMatch(x -> x.subject().equals("www") || x.subject().equals("daemon")), "nologin accounts are fine");
    }

    @Test
    void sshSettingsAreFound() {
        List<Finding> f = scanner.scan();
        for (String rule : List.of("ssh-root-login", "ssh-passwords", "ssh-empty-passwords", "ssh-x11")) {
            assertTrue(has(f, rule, "/etc/ssh/sshd_config"), rule);
        }
    }

    @Test
    void kernelSettingsAreFoundAndAMissingSettingCounts() {
        List<Finding> f = scanner.scan();
        assertTrue(has(f, "no-aslr", "kernel.randomize_va_space"));
        assertTrue(has(f, "accepts-redirects", "net.ipv4.conf.all.accept_redirects"));
        assertTrue(has(f, "no-syncookies", "net.ipv4.tcp_syncookies"));
    }

    @Test
    void onlyPortsOnEveryInterfaceThatAreNotAllowedAreFlagged() {
        List<Finding> f = scanner.scan();
        assertTrue(has(f, "open-port", "port 23") && has(f, "open-port", "port 2375") && has(f, "open-port", "port 6379"));
        assertFalse(has(f, "open-port", "port 22"), "ssh is allowed");
        assertFalse(has(f, "open-port", "port 443"), "https is allowed");
        assertFalse(f.stream().anyMatch(x -> x.subject().equals("port 3306")), "3306 listens on 127.0.0.1 only");
    }

    @Test
    void theMechanicalFixesChangeOnlyPermissions() throws IOException {
        List<Finding> before = scanner.scan();
        int applied = scanner.applyPermissionFixes(before);
        assertEquals(7, applied);
        assertEquals(0640, Modes.of(host.root().resolve("etc/shadow")));
        assertEquals(0600, Modes.of(host.root().resolve("etc/ssh/ssh_host_ed25519_key")));
        assertEquals(0600, Modes.of(host.root().resolve("opt/app/config.properties")));
        assertEquals(0755, Modes.of(host.root().resolve("tmp/.hidden-shell")));
        assertEquals(0775 & ~Modes.GROUP_WRITE, Modes.of(host.root().resolve("etc/cron.daily/backup.sh")));
        assertEquals(0775, Modes.of(host.root().resolve("var/www/uploads")));
        assertEquals(0664, Modes.of(host.root().resolve("var/www/uploads/readme.txt")));
        assertTrue(scanner.scan().stream().noneMatch(Finding::fixable));
        assertEquals(13, scanner.scan().size());
        assertEquals(0, scanner.applyPermissionFixes(scanner.scan()));
    }

    @Test
    void afterTheDecisionsAPersonMakesTheHostIsClean() throws IOException {
        scanner.applyPermissionFixes(scanner.scan());
        host.hardenByHand();
        assertEquals(List.of(), scanner.scan());
    }

    @Test
    void aMissingFileIsNotAnErrorForTheCheckerButAFindingForKernelSettings() throws IOException {
        Files.delete(host.root().resolve("etc/sysctl.conf"));
        Files.delete(host.root().resolve("etc/ssh/sshd_config"));
        List<Finding> f = scanner.scan();
        assertTrue(has(f, "no-aslr", "kernel.randomize_va_space"));
        assertFalse(f.stream().anyMatch(x -> x.rule().startsWith("ssh-")));
    }

    @Test
    void modesPrintInOctal() {
        assertEquals("4755", Modes.octal(04755));
        assertEquals("0640", Modes.octal(0640));
    }

    @Test
    void symbolicLinksAreNotFollowed() throws IOException {
        Path link = host.root().resolve("opt/app/shadow-link");
        try {
            Files.createSymbolicLink(link, host.root().resolve("etc/shadow"));
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "links are not available here");
        }
        assertFalse(scanner.scan().stream().anyMatch(x -> x.subject().equals("/opt/app/shadow-link")));
    }
}
