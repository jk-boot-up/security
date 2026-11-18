package com.jk.explore.security.hostscan;

import com.jk.explore.security.hostscan.Finding.Severity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Reads a running container the way an auditor reads a host: one script run as root inside it, and rules over what it printed. */
final class Scanner {

    static final Set<Integer> ALLOWED_PORTS = Set.of(8080);
    static final Set<String> ALLOWED_SETUID = Set.of();
    private static final Map<Integer, String> DANGEROUS_PORTS = Map.of(23, "telnet: passwords in the clear", 2375, "Docker API without TLS: root on the host for anyone who connects", 6379, "Redis, usually without a password");
    private static final Map<String, Integer> PRIVATE_FILES = Map.of("/etc/shadow", 0640, "/etc/ssh/ssh_host_ed25519_key", 0600, "/opt/app/config.properties", 0600);

    private static final String SNAPSHOT = """
            echo '#modes'
            for p in /etc/shadow /etc/ssh/ssh_host_ed25519_key /opt/app/config.properties /etc/cron.daily/backup.sh; do stat -c '%a %n' $p 2>/dev/null; done
            echo '#setuid'
            find / -xdev -perm -4000 -type f 2>/dev/null
            echo '#writable'
            find / -xdev \\( -path /tmp -o -path /var/tmp \\) -prune -o -perm -0002 ! -type l \\( -type d -o -type f \\) -exec stat -c '%a %F %n' {} \\; 2>/dev/null
            echo '#passwd'
            cat /etc/passwd
            echo '#shadow'
            cat /etc/shadow
            echo '#sshd'
            cat /etc/ssh/sshd_config
            echo '#tcp'
            cat /proc/net/tcp /proc/net/tcp6
            echo '#sysctl'
            for k in net/ipv4/conf/all/accept_redirects net/ipv4/tcp_syncookies; do echo "$k $(cat /proc/sys/$k)"; done
            """;

    private Scanner() {
    }

    /** Runs the snapshot script as root inside the container. */
    static Snapshot read(Host host) throws IOException, InterruptedException {
        return Snapshot.of(host.exec(SNAPSHOT));
    }

    static List<Finding> scan(Host host) throws IOException, InterruptedException {
        return scan(read(host).sections());
    }

    /** Splits the script's output at its #section lines. */
    static Map<String, List<String>> sections(String output) {
        return Snapshot.of(output).sections();
    }

    static List<Finding> scan(Map<String, List<String>> s) {
        List<Finding> found = new ArrayList<>();
        modes(s, found);
        accounts(s, found);
        ssh(s, found);
        ports(s, found);
        sysctl(s, found);
        found.sort(Comparator.comparing(Finding::severity).thenComparing(Finding::rule).thenComparing(Finding::where));
        return found;
    }

    private static void modes(Map<String, List<String>> s, List<Finding> found) {
        for (String line : s.getOrDefault("modes", List.of())) {
            String[] f = line.split(" ", 2);
            int mode = Integer.parseInt(f[0], 8);
            String path = f[1];
            if (PRIVATE_FILES.containsKey(path) && (mode & ~PRIVATE_FILES.get(path)) != 0) {
                found.add(new Finding(Severity.HIGH, "loose-permissions", path, String.format("mode 0%o is looser than 0%o", mode, PRIVATE_FILES.get(path)), "chmod " + Integer.toOctalString(PRIVATE_FILES.get(path)) + " " + path));
            }
            if (path.startsWith("/etc/cron.daily/") && (mode & 0020) != 0) {
                found.add(new Finding(Severity.HIGH, "cron-group-writable", path, String.format("mode 0%o: members of its group can add commands that run as root", mode), "chmod g-w " + path));
            }
        }
        for (String path : s.getOrDefault("setuid", List.of())) {
            if (!ALLOWED_SETUID.contains(path)) {
                found.add(new Finding(Severity.CRITICAL, "unexpected-setuid", path, "mode 4755 runs with its owner's rights and is not on the allow-list", "chmod u-s " + path));
            }
        }
        for (String line : s.getOrDefault("writable", List.of())) {
            String[] f = line.split(" ", 2);
            int mode = Integer.parseInt(f[0], 8);
            String[] rest = f[1].split(" (?=/)", 2);
            String kind = rest[0];
            String path = rest[1];
            boolean dir = kind.equals("directory");
            if (dir && (mode & 01000) == 0) {
                found.add(new Finding(Severity.HIGH, "world-writable-dir", path, String.format("mode 0%o lets any user change it and add or remove entries without the sticky bit", mode), "chmod o-w " + path));
            } else if (!dir) {
                found.add(new Finding(Severity.HIGH, "world-writable-file", path, String.format("mode 0%o lets any user change it", mode), "chmod o-w " + path));
            }
        }
    }

    private static void accounts(Map<String, List<String>> s, List<Finding> found) {
        Snapshot snapshot = new Snapshot(s);
        Map<String, String> shadow = snapshot.passwordHashes();
        for (String[] f : snapshot.accounts()) {
            String name = f[0];
            boolean login = Snapshot.canLogIn(f);
            if (f[2].equals("0") && !name.equals("root")) {
                found.add(new Finding(Severity.CRITICAL, "extra-root-account", name, "UID 0 gives root rights to a second name", null));
            }
            if (login && shadow.getOrDefault(name, "!").isEmpty()) {
                found.add(new Finding(Severity.CRITICAL, "empty-password", name, "can log in with no password", null));
            }
            if (login && Set.of("test", "guest", "admin", "user").contains(name)) {
                found.add(new Finding(Severity.HIGH, "default-account", name, "a well-known account name with a login shell", null));
            }
        }
    }

    private static void ssh(Map<String, List<String>> s, List<Finding> found) {
        String file = "/etc/ssh/sshd_config";
        for (String line : s.getOrDefault("sshd", List.of())) {
            String[] f = line.strip().split("\\s+", 2);
            if (f.length < 2) {
                continue;
            }
            boolean yes = f[1].equals("yes");
            switch (f[0]) {
                case "PermitEmptyPasswords" -> {
                    if (yes) {
                        found.add(new Finding(Severity.CRITICAL, "ssh-empty-passwords", file, "PermitEmptyPasswords yes", null));
                    }
                }
                case "PermitRootLogin" -> {
                    if (yes) {
                        found.add(new Finding(Severity.HIGH, "ssh-root-login", file, "PermitRootLogin yes", null));
                    }
                }
                case "PasswordAuthentication" -> {
                    if (yes) {
                        found.add(new Finding(Severity.MEDIUM, "ssh-passwords", file, "PasswordAuthentication is on: passwords can be guessed, keys cannot", null));
                    }
                }
                case "X11Forwarding" -> {
                    if (yes) {
                        found.add(new Finding(Severity.LOW, "ssh-x11", file, "X11Forwarding yes on a server that needs no graphical sessions", null));
                    }
                }
                default -> {
                }
            }
        }
    }

    /** A listener on every interface (0.0.0.0 or ::) whose port is not on the allow-list. */
    private static void ports(Map<String, List<String>> s, List<Finding> found) {
        TreeSet<Integer> ports = new TreeSet<>();
        for (Listeners.Listener l : Listeners.read(s.getOrDefault("tcp", List.of()))) {
            if (l.everywhere()) {
                ports.add(l.port());
            }
        }
        for (int port : ports) {
            if (!ALLOWED_PORTS.contains(port)) {
                String why = DANGEROUS_PORTS.get(port);
                found.add(new Finding(why != null ? Severity.CRITICAL : Severity.HIGH, "open-port", "port " + port, why != null ? "listens on every interface (" + why + ")" : "listens on every interface and is not on the allow-list", null));
            }
        }
    }

    private static void sysctl(Map<String, List<String>> s, List<Finding> found) {
        for (String line : s.getOrDefault("sysctl", List.of())) {
            String[] f = line.split(" ");
            String key = f[0].replace('/', '.');
            if (key.equals("net.ipv4.conf.all.accept_redirects") && !f[1].equals("0")) {
                found.add(new Finding(Severity.MEDIUM, "accepts-redirects", key, "is " + f[1] + ": a neighbour can change this host's routes", null));
            }
            if (key.equals("net.ipv4.tcp_syncookies") && !f[1].equals("1")) {
                found.add(new Finding(Severity.LOW, "no-syncookies", key, "is not 1: a flood of half-open connections can fill the queue", null));
            }
        }
    }
}
