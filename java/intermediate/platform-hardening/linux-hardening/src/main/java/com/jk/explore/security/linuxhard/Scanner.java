package com.jk.explore.security.linuxhard;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A host checker. It reads a root directory the way it would read {@code /}: file permissions through the JDK, and the text files that describe
 * accounts, the SSH daemon, kernel settings and listening ports. Run against a container's file system, a mounted image or a prepared test tree.
 */
public final class Scanner {

    private final Path root;
    private final Policy policy;

    public Scanner(Path root, Policy policy) {
        this.root = root;
        this.policy = policy;
    }

    public List<Finding> scan() {
        List<Finding> f = new ArrayList<>();
        try {
            permissions(f);
            accounts(f);
            ssh(f);
            kernel(f);
            ports(f);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        f.sort(Comparator.comparing(Finding::severity).reversed().thenComparing(Finding::rule).thenComparing(Finding::subject));
        return f;
    }

    private String rel(Path p) {
        return "/" + root.relativize(p).toString().replace('\\', '/');
    }

    private void permissions(List<Finding> f) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk.filter(x -> !x.equals(root) && !Files.isSymbolicLink(x) && !rel(x).startsWith("/proc"))::iterator) {
                int mode = Modes.of(p);
                String name = rel(p);
                boolean dir = Files.isDirectory(p);
                if (!dir && (mode & (Modes.SETUID | Modes.SETGID)) != 0 && !policy.setuidAllowed().contains(name)) {
                    f.add(new Finding("unexpected-setuid", Severity.CRITICAL, name, "mode " + Modes.octal(mode) + " runs with its owner's rights and is not on the allow-list", true));
                } else if ((mode & Modes.OTHER_WRITE) != 0 && !(dir && (mode & Modes.STICKY) != 0)) {
                    f.add(new Finding(dir ? "world-writable-dir" : "world-writable-file", dir ? Severity.HIGH : Severity.HIGH, name,
                            "mode " + Modes.octal(mode) + " lets any user change it" + (dir ? " and add or remove entries without the sticky bit" : ""), true));
                }
                Integer max = policy.maxModes().get(name);
                if (max != null && (mode & ~max) != 0 && (mode & Modes.OTHER_WRITE) == 0 && (mode & (Modes.SETUID | Modes.SETGID)) == 0) {
                    f.add(new Finding("loose-permissions", name.contains("key") || name.contains("shadow") || name.contains("config") ? Severity.HIGH : Severity.MEDIUM, name,
                            "mode " + Modes.octal(mode) + " is looser than " + Modes.octal(max), true));
                }
                if (name.startsWith("/etc/cron") && !dir && (mode & (Modes.GROUP_WRITE | Modes.OTHER_WRITE)) != 0 && (mode & Modes.OTHER_WRITE) == 0) {
                    f.add(new Finding("cron-group-writable", Severity.HIGH, name, "mode " + Modes.octal(mode) + ": members of its group can add commands that run as root", true));
                }
            }
        }
    }

    private void accounts(List<Finding> f) throws IOException {
        Map<String, String> hashes = HostFiles.passwordHashes(root);
        for (String[] account : HostFiles.passwd(root)) {
            String user = account[0];
            boolean login = HostFiles.canLogIn(account);
            if (account[2].equals("0") && !user.equals("root")) {
                f.add(new Finding("extra-root-account", Severity.CRITICAL, user, "UID 0 gives root rights to a second name", false));
            }
            String hash = hashes.get(user);
            if (login && hash != null && hash.isEmpty()) {
                f.add(new Finding("empty-password", Severity.CRITICAL, user, "can log in with no password", false));
            }
            if (login && policy.defaultAccountNames().contains(user)) {
                f.add(new Finding("default-account", Severity.HIGH, user, "a well-known account name with a login shell", false));
            }
        }
    }

    private void ssh(List<Finding> f) throws IOException {
        if (!Files.exists(root.resolve("etc/ssh/sshd_config"))) {
            return;
        }
        Map<String, String> s = HostFiles.sshd(root);
        if (!"no".equals(s.getOrDefault("permitrootlogin", "yes")) && !"prohibit-password".equals(s.get("permitrootlogin"))) {
            f.add(new Finding("ssh-root-login", Severity.HIGH, "/etc/ssh/sshd_config", "PermitRootLogin " + s.getOrDefault("permitrootlogin", "(default: yes)"), false));
        }
        if (!"no".equals(s.getOrDefault("passwordauthentication", "yes"))) {
            f.add(new Finding("ssh-passwords", Severity.MEDIUM, "/etc/ssh/sshd_config", "PasswordAuthentication is on: passwords can be guessed, keys cannot", false));
        }
        if ("yes".equals(s.get("permitemptypasswords"))) {
            f.add(new Finding("ssh-empty-passwords", Severity.CRITICAL, "/etc/ssh/sshd_config", "PermitEmptyPasswords yes", false));
        }
        if ("yes".equals(s.get("x11forwarding"))) {
            f.add(new Finding("ssh-x11", Severity.LOW, "/etc/ssh/sshd_config", "X11Forwarding yes on a server that needs no graphical sessions", false));
        }
    }

    private void kernel(List<Finding> f) throws IOException {
        Map<String, String> s = HostFiles.sysctl(root);
        if (!"2".equals(s.get("kernel.randomize_va_space"))) {
            f.add(new Finding("no-aslr", Severity.MEDIUM, "kernel.randomize_va_space", "is " + s.getOrDefault("kernel.randomize_va_space", "(unset)") + ", not 2: memory layout is not randomized", false));
        }
        if (!"1".equals(s.get("net.ipv4.tcp_syncookies"))) {
            f.add(new Finding("no-syncookies", Severity.LOW, "net.ipv4.tcp_syncookies", "is not 1: a flood of half-open connections can fill the queue", false));
        }
        if ("1".equals(s.get("net.ipv4.conf.all.accept_redirects"))) {
            f.add(new Finding("accepts-redirects", Severity.MEDIUM, "net.ipv4.conf.all.accept_redirects", "is 1: a neighbour can change this host's routes", false));
        }
    }

    private void ports(List<Finding> f) throws IOException {
        for (PortTable.Listener l : PortTable.listening(root.resolve("proc/net/tcp"))) {
            int port = l.port();
            if (l.everywhere() && !policy.portsAllowed().contains(port)) {
                f.add(new Finding("open-port", port == 23 || port == 2375 || port == 6379 ? Severity.CRITICAL : Severity.HIGH, "port " + port,
                        "listens on every interface (" + describe(port) + ")", false));
            }
        }
    }

    private static String describe(int port) {
        return switch (port) {
            case 23 -> "telnet: passwords in the clear";
            case 2375 -> "Docker API without TLS: root on the host for anyone who connects";
            case 3306 -> "MySQL";
            case 5432 -> "PostgreSQL";
            case 6379 -> "Redis, usually without a password";
            case 8080 -> "an admin or debug web service";
            default -> "not on the allow-list";
        };
    }

    /** Applies the safe, mechanical fixes: a permission change for each fixable finding. Returns the changes made. */
    public List<Fix> applyPermissionFixes(List<Finding> findings) {
        List<Fix> done = new ArrayList<>();
        try {
            for (Finding x : findings) {
                if (!x.fixable()) {
                    continue;
                }
                Path p = root.resolve(x.subject().substring(1));
                int mode = Modes.of(p);
                Fix fix = switch (x.rule()) {
                    case "unexpected-setuid" -> remove(x, mode, Modes.SETUID | Modes.SETGID, "set-user-id");
                    case "world-writable-dir", "world-writable-file" -> remove(x, mode, Modes.OTHER_WRITE, "other write");
                    case "cron-group-writable" -> remove(x, mode, Modes.GROUP_WRITE, "group write");
                    case "loose-permissions" -> keep(x, mode, policy.maxModes().get(x.subject()));
                    default -> null;
                };
                if (fix != null && fix.after() != mode) {
                    Modes.set(p, fix.after());
                    done.add(fix);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return done;
    }

    /** Takes bits away: 4755 - 4000 = 0755. */
    private static Fix remove(Finding x, int mode, int bits, String why) {
        return new Fix(x.subject(), mode, "-", mode & bits, mode & ~bits, "no " + why);
    }

    /** Keeps only the bits the policy allows: 0644 & 0600 = 0600. */
    private static Fix keep(Finding x, int mode, int max) {
        return new Fix(x.subject(), mode, "&", max, mode & max, "policy " + Modes.octal(max));
    }
}
