package com.jk.explore.security.linuxhard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs the host-checking story in four numbered acts and prints exact, repeatable results. Each act shows its input and its
 * result in separate, labelled sections. The file tree is a temporary directory whose name is never printed.
 */
public class LinuxHardeningDemo {

    private static final List<String> GROUPS = List.of("accounts", "sshd", "kernel", "ports");

    public static void main(String[] args) throws IOException {
        System.out.print(run());
    }

    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        try (Fixture host = Fixture.insecure()) {
            Path root = host.root();
            Policy policy = Policy.standard();
            Scanner scanner = new Scanner(root, policy);

            out.append("ONE. The shop's server image, before any fix.\n");
            out.append("  HOW A MODE IS READ, DIGIT BY DIGIT:\n");
            int a = Modes.of(root.resolve("tmp/.hidden-shell"));
            int b = Modes.of(root.resolve("etc/shadow"));
            row(out, "file", "/tmp/.hidden-shell", "/etc/shadow");
            row(out, "mode", Modes.octal(a), Modes.octal(b));
            String[] names = {"special", "user", "group", "other"};
            for (int i = 0; i < 4; i++) {
                int shift = 9 - 3 * i;
                int da = a >> shift & 7;
                int db = b >> shift & 7;
                row(out, names[i], da + " " + (i == 0 ? Modes.special(da) : Modes.rwx(da)), db + " " + (i == 0 ? Modes.special(db) : Modes.rwx(db)));
            }
            row(out, "policy", policy.setuidAllowed().contains("/tmp/.hidden-shell") ? "allowed" : "not allowed",
                    "at most " + Modes.octal(policy.maxModes().get("/etc/shadow")));
            List<Finding> first = scanner.scan();
            out.append("  THE REPORT: ").append(first.size()).append(" FINDINGS, WORST FIRST (auto = a machine can fix):\n");
            first.forEach(f -> out.append("    ").append(f).append('\n'));
            out.append("  THE COUNT:\n");
            out.append("    severity  ").append(count(first, Severity.CRITICAL)).append(" critical + ").append(count(first, Severity.HIGH)).append(" high + ")
                    .append(count(first, Severity.MEDIUM)).append(" medium + ").append(count(first, Severity.LOW)).append(" low = ").append(first.size()).append('\n');
            long auto = first.stream().filter(Finding::fixable).count();
            out.append("    machine   ").append(auto).append(" auto, all of them file permissions\n");
            Map<String, Long> byGroup = first.stream().filter(f -> !f.fixable()).collect(Collectors.groupingBy(LinuxHardeningDemo::group, Collectors.counting()));
            out.append("    person    ").append(first.size() - auto).append(" = ")
                    .append(GROUPS.stream().map(g -> byGroup.getOrDefault(g, 0L) + " " + g).collect(Collectors.joining(" + "))).append('\n');

            out.append("TWO. The fixes a machine can make.\n");
            out.append("  BEFORE THE FIX:\n");
            out.append("    ").append(first.size()).append(" findings, ").append(auto).append(" of them marked auto\n");
            List<Fix> fixes = scanner.applyPermissionFixes(first);
            out.append("  HOW EACH NEW MODE IS MADE (- takes bits away, & keeps only):\n");
            fixes.forEach(x -> out.append(String.format("    %-29s %s  %s%n", x.path(), x.steps(), x.why())));
            List<Finding> second = scanner.scan();
            out.append("  AFTER THE FIX:\n");
            out.append("    changes    ").append(fixes.size()).append('\n');
            out.append("    findings   ").append(first.size()).append(" - ").append(fixes.size()).append(" = ").append(second.size())
                    .append(second.stream().anyMatch(Finding::fixable) ? "" : ", none of them a permission").append('\n');
            out.append("    run again  ").append(scanner.applyPermissionFixes(second).size()).append(" changes: nothing left to fix\n");

            out.append("THREE. The decisions a person makes.\n");
            out.append("  HOW THE ACCOUNT LINES ARE READ (/etc/passwd, /etc/shadow):\n");
            String[] toor = HostFiles.passwd(root).stream().filter(p -> p[0].equals("toor")).findFirst().orElseThrow();
            out.append("    ").append(String.join(":", toor)).append('\n');
            out.append("      name ").append(toor[0]).append(", UID ").append(toor[2]).append(" = root's number -> a second root\n");
            String test = Files.readAllLines(root.resolve("etc/shadow")).stream().filter(l -> l.startsWith("test:")).findFirst().orElseThrow();
            out.append("    ").append(test).append('\n');
            out.append("      name test, password field ").append(HostFiles.passwordHashes(root).get("test").isEmpty() ? "empty -> no password" : "set").append('\n');
            out.append("  HOW THE PORT TABLE IS READ (/proc/net/tcp, hex):\n");
            for (PortTable.Listener l : PortTable.listening(root.resolve("proc/net/tcp"))) {
                String hexPort = l.local().substring(l.local().indexOf(':') + 1);
                String verdict = !l.everywhere() ? "this host only" : policy.portsAllowed().contains(l.port()) ? "allowed" : "not allowed";
                out.append(String.format("    %-14s %-10s 0x%s = %-5d %s%n", l.local(), l.address(), hexPort, l.port(), verdict));
            }
            out.append("  BEFORE THE DECISIONS (").append(second.size()).append(" findings):\n");
            settings(out, root);
            host.hardenByHand();
            List<Finding> third = scanner.scan();
            out.append("  AFTER THE DECISIONS (").append(third.size()).append(" findings):\n");
            settings(out, root);

            out.append("FOUR. Running it again, in every build.\n");
            BuildGate gate = new BuildGate(Severity.HIGH);
            out.append("  THE BUILD RULE:\n");
            out.append("    fail at   ").append(gate.failAt()).append(" or worse\n");
            out.append("  THE HARDENED IMAGE:\n");
            out.append("    findings  ").append(third.size()).append(" -> build ").append(gate.passes(third) ? "passes" : "FAILS").append('\n');
            host.regress();
            out.append("  A LATER CHANGE:\n");
            out.append("    a deployment script runs chmod 0777 /var/www/uploads\n");
            out.append("    an engineer adds a test account with a login shell\n");
            List<Finding> fourth = scanner.scan();
            out.append("  THE SCAN IN THE BUILD:\n");
            fourth.forEach(f -> out.append("    ").append(f).append('\n'));
            out.append("    findings  ").append(fourth.size()).append(" -> build ").append(gate.passes(fourth) ? "passes" : "FAILS").append('\n');
        }
        return out.toString();
    }

    private static void row(StringBuilder out, String key, String left, String right) {
        out.append(String.format("    %-9s%-21s%s%n", key, left, right));
    }

    /** The settings a person decides on, one per line, read from the image's files. */
    private static void settings(StringBuilder out, Path root) throws IOException {
        String logins = HostFiles.passwd(root).stream().filter(HostFiles::canLogIn).map(p -> p[0]).collect(Collectors.joining(", "));
        out.append(String.format("    %-23s %s%n", "login accounts", logins));
        Map<String, String> sshd = HostFiles.sshd(root);
        for (String k : List.of("PermitRootLogin", "PasswordAuthentication", "PermitEmptyPasswords", "X11Forwarding")) {
            out.append(String.format("    %-23s %s%n", k, sshd.getOrDefault(k.toLowerCase(), "(unset)")));
        }
        Map<String, String> sysctl = HostFiles.sysctl(root);
        for (String k : List.of("kernel.randomize_va_space", "net.ipv4.tcp_syncookies", "net.ipv4.conf.all.accept_redirects")) {
            out.append(String.format("    %-23s %s%n", k.substring(k.lastIndexOf('.') + 1), sysctl.getOrDefault(k, "(unset)")));
        }
        String ports = PortTable.listening(root.resolve("proc/net/tcp")).stream().filter(PortTable.Listener::everywhere)
                .map(PortTable.Listener::port).sorted().map(String::valueOf).collect(Collectors.joining(" "));
        out.append(String.format("    %-23s %s%n", "ports, every interface", ports));
    }

    /** Which part of the host a finding is about. */
    static String group(Finding f) {
        return switch (f.rule()) {
            case "extra-root-account", "empty-password", "default-account" -> "accounts";
            case "no-aslr", "no-syncookies", "accepts-redirects" -> "kernel";
            case "open-port" -> "ports";
            default -> f.rule().startsWith("ssh-") ? "sshd" : "files";
        };
    }

    private static long count(List<Finding> f, Severity s) {
        return f.stream().filter(x -> x.severity() == s).count();
    }
}
