package com.jk.explore.security.hostscan;

import com.jk.explore.security.hostscan.Finding.Severity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the two images, starts containers from them and prints the story in five numbered acts. Each act shows its
 * input and its result in separate, labelled sections. Container names are never printed.
 */
public class HostScanDemo {

    static final List<String> TYPICAL_RUN = List.of("net.ipv4.conf.all.accept_redirects=1", "net.ipv4.tcp_syncookies=0");
    static final List<String> HARDENED_RUN = List.of("net.ipv4.conf.all.accept_redirects=0", "net.ipv4.tcp_syncookies=1");
    private static final List<String> GROUPS = List.of("accounts", "sshd", "kernel", "ports");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Images.buildTypical();
        Images.buildHardened();
        act1(out);
        act2(out);
        act3(out);
        act4(out);
        act5(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void fact(StringBuilder out, String key, Object value) {
        line(out, String.format("    %-13s %s", key, value));
    }

    /** What the script read, as one fact per line: the input every rule works on. */
    static void describe(StringBuilder out, Snapshot s) {
        fact(out, "UID 0", String.join(", ", s.uidZero()));
        fact(out, "no password", s.emptyPasswords().isEmpty() ? "none" : String.join(", ", s.emptyPasswords()));
        fact(out, "set-user-id", s.lines("setuid").isEmpty() ? "none" : String.join(", ", s.lines("setuid")));
        List<String> writable = s.lines("writable").stream().map(l -> l.split(" ")[0] + " " + l.substring(l.indexOf(" /") + 1)).toList();
        if (writable.isEmpty()) {
            fact(out, "world-write", "none");
        }
        writable.forEach(w -> fact(out, "world-write", w));
        s.lines("modes").forEach(m -> fact(out, "mode", m));
        s.lines("sshd").stream().filter(l -> !l.startsWith("Port")).forEach(l -> fact(out, "sshd", l));
        s.lines("sysctl").forEach(l -> fact(out, "sysctl", l.substring(l.lastIndexOf('/') + 1)));
    }

    static String summary(List<Finding> findings) {
        return count(findings, Severity.CRITICAL) + " critical + " + count(findings, Severity.HIGH) + " high + " + count(findings, Severity.MEDIUM) + " medium + "
                + count(findings, Severity.LOW) + " low = " + findings.size();
    }

    private static long count(List<Finding> f, Severity s) {
        return f.stream().filter(x -> x.severity() == s).count();
    }

    /** Which part of the host a finding is about. */
    static String group(Finding f) {
        return switch (f.rule()) {
            case "extra-root-account", "empty-password", "default-account" -> "accounts";
            case "accepts-redirects", "no-syncookies" -> "kernel";
            case "open-port" -> "ports";
            default -> f.rule().startsWith("ssh-") ? "sshd" : "files";
        };
    }

    private static String user(String image) throws Exception {
        return Docker.run("inspect", "-f", "{{if .Config.User}}{{.Config.User}}{{else}}root{{end}}", image).strip();
    }

    private static void act1(StringBuilder out) throws Exception {
        line(out, "ONE. What the scan finds in a typical image.");
        try (Host host = new Host(Images.TYPICAL, TYPICAL_RUN)) {
            Snapshot s = Scanner.read(host);
            line(out, "  THE CONTAINER, AS THE SCRIPT READ IT (" + Images.TYPICAL + "):");
            describe(out, s);
            line(out, "  HOW THE LISTENERS ARE READ (/proc/net/tcp6, hex):");
            for (Listeners.Listener l : s.listeners()) {
                String verdict = !l.everywhere() ? "one address only" : Scanner.ALLOWED_PORTS.contains(l.port()) ? "allowed" : "not allowed";
                line(out, String.format("    %-3s %s  0x%s = %-5d %s", l.address(), l.everywhere() ? "every interface" : "one address    ", l.hexPort(), l.port(), verdict));
            }
            List<Finding> findings = Scanner.scan(s.sections());
            line(out, "  THE REPORT: " + findings.size() + " FINDINGS, WORST FIRST (auto = a machine can fix):");
            findings.forEach(f -> line(out, "    " + f));
            line(out, "  THE COUNT:");
            fact(out, "severity", summary(findings));
            long auto = findings.stream().filter(Finding::autoFixable).count();
            fact(out, "machine", auto + " auto, all of them file permissions");
            Map<String, Long> byGroup = findings.stream().filter(f -> !f.autoFixable()).collect(Collectors.groupingBy(HostScanDemo::group, Collectors.counting()));
            fact(out, "person", (findings.size() - auto) + " = " + GROUPS.stream().map(g -> byGroup.getOrDefault(g, 0L) + " " + g).collect(Collectors.joining(" + ")));
        }
    }

    private static void act2(StringBuilder out) throws Exception {
        line(out, "TWO. The fixes a machine can make, and where they live.");
        List<String> fixed;
        int before;
        try (Host host = new Host(Images.TYPICAL, TYPICAL_RUN)) {
            List<Finding> findings = Scanner.scan(host);
            before = findings.size();
            List<Finding> auto = findings.stream().filter(Finding::autoFixable).toList();
            line(out, "  THE FIX COMMANDS (run as root in the running container):");
            for (Finding f : auto) {
                host.exec(f.fix());
                line(out, "    " + f.fix());
            }
            fixed = auto.stream().map(Finding::where).toList();
            List<Finding> after = Scanner.scan(host);
            line(out, "  AFTER THE FIX, IN THE SAME CONTAINER:");
            fact(out, "findings", before + " - " + auto.size() + " = " + after.size() + (after.stream().anyMatch(Finding::autoFixable) ? "" : ", none of them a permission"));
            List<String> diff = Docker.run("diff", host.name()).lines().toList();
            line(out, "  WHERE THE FIX LIVES (docker diff: the container's own layer):");
            diff.stream().filter(l -> fixed.contains(l.substring(2))).sorted().forEach(l -> line(out, "    " + l));
            long parents = diff.stream().filter(l -> !fixed.contains(l.substring(2))).count();
            line(out, "    and " + parents + " parent folders of those files");
        }
        try (Host fresh = new Host(Images.TYPICAL, TYPICAL_RUN)) {
            line(out, "  A NEW CONTAINER FROM THE SAME IMAGE:");
            fact(out, "findings", Scanner.scan(fresh).size() + ": the fixes stayed in the old container's layer");
        }
    }

    private static void act3(StringBuilder out) throws Exception {
        line(out, "THREE. The decisions a person makes, in the recipe.");
        line(out, "  THE DECISIONS IN THE RECIPE (" + Images.HARDENED + "):");
        line(out, "    accounts   no toor, no test; app has the shell /sbin/nologin");
        line(out, "    modes      600 key and config, 750 cron; no copied shell");
        line(out, "    sshd       the four settings set to no");
        line(out, "    listeners  one: port 8080");
        line(out, "    process    USER app");
        line(out, "    started    --sysctl accept_redirects=0, tcp_syncookies=1");
        try (Host host = new Host(Images.HARDENED, HARDENED_RUN)) {
            Snapshot s = Scanner.read(host);
            line(out, "  THE CONTAINER, AS THE SCRIPT READ IT (" + Images.HARDENED + "):");
            describe(out, s);
            fact(out, "listening", s.listeners().stream().map(l -> l.address() + " port " + l.port()).collect(Collectors.joining(", ")));
            line(out, "  THE SCAN:");
            fact(out, "findings", Scanner.scan(s.sections()).size());
            fact(out, "process", user(Images.TYPICAL) + " in the typical image, " + user(Images.HARDENED) + " in the hardened one");
            fact(out, "id -un", Docker.run("exec", host.name(), "id", "-un").strip() + " (asked inside the hardened container)");
        }
    }

    private static void act4(StringBuilder out) throws Exception {
        line(out, "FOUR. Running it again.");
        String file = "/opt/app/config.properties";
        try (Host host = new Host(Images.HARDENED, HARDENED_RUN)) {
            line(out, "  BEFORE THE DRIFT (a running hardened container):");
            fact(out, "findings", Scanner.scan(host).size());
            line(out, "  THE DRIFT (someone, inside that container):");
            String command = "chmod 666 " + file;
            host.exec(command);
            line(out, "    " + command);
            Snapshot s = Scanner.read(host);
            line(out, "  AFTER THE DRIFT, THE SAME SCAN:");
            Scanner.scan(s.sections()).forEach(f -> line(out, "    " + f));
            int mode = Integer.parseInt(s.lines("modes").stream().filter(m -> m.endsWith(file)).findFirst().orElseThrow().split(" ")[0], 8);
            int max = 0600;
            line(out, String.format("    too open      0%o & ~0%o = 00%o: rw- for group and others", mode, max, mode & ~max));
        }
        try (Host fresh = new Host(Images.HARDENED, HARDENED_RUN)) {
            line(out, "  A NEW CONTAINER FROM THE IMAGE:");
            fact(out, "findings", Scanner.scan(fresh).size() + ": the answer to drift is a new container");
        }
    }

    private static void act5(StringBuilder out) throws Exception {
        line(out, "FIVE. The bill.");
        try (Host host = new Host(Images.TYPICAL, TYPICAL_RUN)) {
            line(out, "  THE KERNEL, FROM INSIDE THE CONTAINER:");
            String aslr = host.exec("cat /proc/sys/kernel/randomize_va_space").strip();
            fact(out, "read", "kernel.randomize_va_space = " + aslr + " (the host's value)");
            String attempt;
            try {
                host.exec("echo 0 > /proc/sys/kernel/randomize_va_space");
                attempt = "changed";
            } catch (IllegalStateException e) {
                attempt = e.getMessage().contains("Read-only file system") ? "refused: read-only file system" : "refused";
            }
            fact(out, "write 0", attempt);
        }
        line(out, "  WHAT IT MEANS:");
        line(out, "    a container shares the host's kernel: the host fixes these");
        line(out, "    the scan ran as root; an attacker starts as app, sees less");
        line(out, "    the allow-lists (port 8080, no set-user-id) are the shop's");
        line(out, "    how a container is started: the container-hardening project");
    }
}
