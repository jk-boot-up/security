package com.jk.explore.security.hostscan;

import com.jk.explore.security.hostscan.Finding.Severity;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HostScanDemo {

    static final List<String> TYPICAL_RUN = List.of("net.ipv4.conf.all.accept_redirects=1", "net.ipv4.tcp_syncookies=0");
    static final List<String> HARDENED_RUN = List.of("net.ipv4.conf.all.accept_redirects=0", "net.ipv4.tcp_syncookies=1");

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

    static String summary(List<Finding> findings) {
        Map<Severity, Integer> counts = new TreeMap<>();
        findings.forEach(f -> counts.merge(f.severity(), 1, Integer::sum));
        long fixable = findings.stream().filter(Finding::autoFixable).count();
        return findings.size() + " findings: " + counts.getOrDefault(Severity.CRITICAL, 0) + " critical, " + counts.getOrDefault(Severity.HIGH, 0) + " high, " + counts.getOrDefault(Severity.MEDIUM, 0) + " medium, "
                + counts.getOrDefault(Severity.LOW, 0) + " low; " + fixable + " can be fixed mechanically";
    }

    private static void act1(StringBuilder out) throws Exception {
        line(out, "ONE. What the scan finds in a typical image.");
        try (Host host = new Host(Images.TYPICAL, TYPICAL_RUN)) {
            line(out, "  shop-host:typical is a real container from a real image, scanned by one script run as root inside it: files and modes, accounts, sshd_config, listeners from /proc/net/tcp, kernel settings");
            List<Finding> findings = Scanner.scan(host);
            findings.forEach(f -> line(out, "  " + f));
            line(out, "  " + summary(findings));
        }
    }

    private static void act2(StringBuilder out) throws Exception {
        line(out, "TWO. The fixes a machine can make, and where they live.");
        String name;
        try (Host host = new Host(Images.TYPICAL, TYPICAL_RUN)) {
            name = host.name();
            List<Finding> before = Scanner.scan(host);
            int applied = 0;
            for (Finding f : before) {
                if (f.autoFixable()) {
                    host.exec(f.fix());
                    applied++;
                }
            }
            List<Finding> after = Scanner.scan(host);
            line(out, "  permission changes applied in the running container: " + applied);
            line(out, "  findings left: " + after.size() + " (none of them a permission)");
            after.forEach(f -> line(out, "    " + f.rule() + " - " + f.where()));
            List<String> changed = Docker.run("diff", host.name()).lines().map(l -> l.substring(2)).filter(p -> !p.equals("/etc") && !p.equals("/tmp") && !p.equals("/var") && !p.equals("/var/www") && !p.equals("/opt") && !p.equals("/opt/app")).sorted().toList();
            line(out, "  docker diff lists what changed in the container's own writable layer: " + changed);
        }
        try (Host fresh = new Host(Images.TYPICAL, TYPICAL_RUN)) {
            line(out, "  a new container from the same image: " + summary(Scanner.scan(fresh)).replaceAll(";.*", "") + "  <- A FIX IN A RUNNING CONTAINER IS NOT A FIX");
        }
        line(out, "  the image is what runs next time, so a change belongs in the image's recipe");
    }

    private static void act3(StringBuilder out) throws Exception {
        line(out, "THREE. The decisions a person makes, in the recipe.");
        line(out, "  no second root account and no test account; a locked service account with no login shell; sshd_config with root login, passwords, empty passwords and X11 off;");
        line(out, "  private modes for the key, the config and cron; no copied shell with the set-user-id bit; one listener, port 8080; the process runs as app, not root;");
        line(out, "  and the namespaced kernel settings the container is started with: accept_redirects 0 and syncookies 1");
        try (Host host = new Host(Images.HARDENED, HARDENED_RUN)) {
            List<Finding> findings = Scanner.scan(host);
            line(out, "  shop-host:hardened, scanned the same way: " + findings.size() + " findings");
            String user = Docker.run("inspect", "-f", "{{if .Config.User}}{{.Config.User}}{{else}}root{{end}}", Images.HARDENED).strip();
            String typicalUser = Docker.run("inspect", "-f", "{{if .Config.User}}{{.Config.User}}{{else}}root{{end}}", Images.TYPICAL).strip();
            line(out, "  the user the image's process runs as: " + typicalUser + " in the typical image, " + user + " in the hardened one");
            line(out, "  the process of the hardened container runs as: " + Docker.run("exec", host.name(), "id", "-un").strip());
        }
    }

    private static void act4(StringBuilder out) throws Exception {
        line(out, "FOUR. Running it again.");
        try (Host host = new Host(Images.HARDENED, HARDENED_RUN)) {
            host.exec("chmod 666 /opt/app/config.properties");
            List<Finding> drift = Scanner.scan(host);
            line(out, "  someone loosens the config file inside a running container: " + drift.stream().map(f -> f.rule() + " - " + f.where()).toList());
        }
        try (Host fresh = new Host(Images.HARDENED, HARDENED_RUN)) {
            line(out, "  a new container from the image: " + Scanner.scan(fresh).size() + " findings");
        }
        line(out, "  the scan is part of the build, on the image after it is built, and a regression fails it; on running containers it finds drift, and the answer to drift is a new container");
    }

    private static void act5(StringBuilder out) throws Exception {
        line(out, "FIVE. The bill.");
        try (Host host = new Host(Images.TYPICAL, TYPICAL_RUN)) {
            String aslr = host.exec("cat /proc/sys/kernel/randomize_va_space").strip();
            String attempt;
            try {
                host.exec("echo 0 > /proc/sys/kernel/randomize_va_space");
                attempt = "changed";
            } catch (IllegalStateException e) {
                attempt = e.getMessage().contains("Read-only file system") ? "refused (read-only file system)" : "refused";
            }
            line(out, "  kernel.randomize_va_space inside the container is the host kernel's: " + aslr + "; writing 0 to it from inside: " + attempt + "  <- A CONTAINER SHARES THE HOST'S KERNEL, SO THESE CHECKS ARE THE HOST'S TO FIX");
            line(out, "  the scan runs as root inside the container, so it reads what a root process would; a process that runs as app sees less, and an attacker starts as that process");
        }
        line(out, "  the allow-lists, port 8080 and no set-user-id file, are the shop's own decisions; a scan with the wrong lists is either silent or noisy");
        line(out, "  an image scan sees one moment of one container; what is added at start-up, or by a volume or an environment variable, is another scan");
        line(out, "  how a container is started, with which user, capabilities, mounts and filesystem mode, is the subject of container-hardening");
    }
}
