package com.jk.explore.security.runtimesec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RuntimeSecurityDemo {

    /** How long a command keeps its input open, so the sensor still finds its container when it reads the record. */
    private static final long HOLD = 8_000;

    private final StringBuilder out = new StringBuilder();
    private String outsideIp = "";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        return new RuntimeSecurityDemo().demo();
    }

    static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

    private void act(String heading) {
        out.append(heading).append('\n');
    }

    /** A labelled section of an act: its header, then its lines. */
    private void section(String header, List<String> lines) {
        out.append("  ").append(header).append('\n');
        lines.forEach(l -> out.append("    ").append(l).append('\n'));
    }

    private void section(String header, String... lines) {
        section(header, List.of(lines));
    }

    static String kv(String key, Object value) {
        return String.format("%-10s %s", key, value);
    }

    /** The alerts as rule, pod and detail lines; addresses are named, never printed. */
    static List<String> alerts(Set<Alert> alerts, String outsideIp) {
        if (alerts.isEmpty()) {
            return List.of("none");
        }
        List<String> lines = new ArrayList<>();
        for (Alert a : alerts) {
            lines.add(kv("rule", a.rule()));
            lines.add(kv("pod", a.pod()));
            lines.add(kv("detail", outsideIp.isEmpty() ? a.detail() : a.detail().replace(outsideIp, "the outside server")));
        }
        return lines;
    }

    private String demo() throws Exception {
        falco();
        try (Lab lab = new Lab()) {
            outsideIp = lab.outsideIp;
            work(lab);
            shell(lab);
            write(lab);
            connect(lab);
        }
        return out.toString();
    }

    private void falco() throws Exception {
        act("ONE. Falco on this machine.");
        Path rules = Path.of("rules");
        String yaml = Files.readString(rules.resolve("bank-runtime-rules.yaml"));
        section("THE RULES (rules/bank-runtime-rules.yaml):", Falco.ruleNames(yaml));
        section("FALCO " + Falco.version() + " CHECKS THE RULES (falco --validate):",
                kv("accepted", yesNo(Falco.validates(rules, "bank-runtime-rules.yaml"))));
        section("FALCO'S DRIVER ON THIS KERNEL:",
                kv("BTF", yesNo(Falco.kernelHasBtf()) + " (the kernel's type information)"),
                kv("driver", "modern eBPF"),
                kv("result", Falco.tryDriver()));
        section("SO THE SAME THREE TESTS RUN ON LINUX AUDIT RECORDS:",
                String.format("%-8s %-17s -> %s", "exec", "a program starts", Rules.SHELL),
                String.format("%-8s %-17s -> %s", "write", "a file is written", Rules.WRITE),
                String.format("%-8s %-17s -> %s", "connect", "a connection", Rules.CONNECT));
    }

    private void work(Lab lab) throws Exception {
        act("TWO. The payments service doing its normal work.");
        section("THE BASELINE (what " + Pods.API + " is expected to do):",
                kv("writes", String.join(", ", lab.baseline.writablePrefixes())),
                kv("connects", lab.baseline.allowedIps().size() + " addresses: the ledger-db service, the DNS"),
                kv("shells", "none: it is started as httpd, not by a shell"));
        section("THE WORK (three commands in " + Pods.API + "):",
                String.format("%-32s %s", "wget http://localhost:8080/", "a call to itself"),
                String.format("%-32s %s", "tee /var/log/payments/audit.log", "a write to its log"),
                String.format("%-32s %s", "nc ledger-db 5432", "a call to the ledger"));
        lab.cluster.exec(Pods.API, "wget", "-q", "-O", "/dev/null", "http://localhost:8080/");
        held(lab, Pods.API, "settled payment\n", "tee", "/var/log/payments/audit.log");
        held(lab, Pods.API, "", "nc", "-w", "2", Pods.LEDGER, "5432");
        Set<String> kinds = lab.seenFrom(Pods.API);
        section("WHAT THE SENSOR SAW (audit records of the pod):",
                List.of("exec", "write", "connect").stream().map(k -> kv(k, yesNo(kinds.contains(k)))).toList());
        section("THE ALERTS:", alerts(lab.newAlerts(), outsideIp));
        Event ledger = lab.lastConnect(Pods.API, 5432);
        List<String> how = new ArrayList<>(Audit.explain(Audit.saddr(lab.sensor.log(), ledger.id())));
        how.add(kv("process", ledger.comm() + ", in container cri-containerd-<64 hex>"));
        how.add(kv("pod", Pods.API + " (kubectl maps the container to it)"));
        how.add(kv("baseline", "the ledger-db address is on it: no alert"));
        section("HOW ONE RECORD IS READ (the call to the ledger):", how);
    }

    private void shell(Lab lab) throws Exception {
        act("THREE. A shell in the container.");
        section("THE ACTION (someone with kubectl exec rights):",
                "kubectl exec -i " + Pods.API + " -- sh", "then types: id -u");
        Sh.Result id = held(lab, Pods.API, "id -u\n", "sh");
        section("WHAT THE SHELL ANSWERED:", kv("id -u", id.output().trim() + " (the root user)"));
        section("THE ALERTS:", alerts(lab.newAlerts(), outsideIp));
        section("WHY:", kv("exec", "a process named sh started"),
                kv("shells", String.join(" ", Rules.SHELLS.stream().sorted().toList())),
                kv("baseline", "no shell, so every shell is an alert"));
    }

    private void write(Lab lab) throws Exception {
        act("FOUR. A write where the service never writes.");
        section("THE ACTION (in " + Pods.API + "):", "tee /usr/bin/miner   a program, written to /usr/bin");
        held(lab, Pods.API, "#!/bin/sh\n", "tee", "/usr/bin/miner");
        section("THE ALERTS:", alerts(lab.newAlerts(), outsideIp));
        section("WHY:", kv("path", "/usr/bin/miner"),
                kv("allowed", String.join(", ", lab.baseline.writablePrefixes())),
                kv("starts", "with none of them, so it is an alert"));
        section("THE SAME ACTION (in " + Pods.API_RO + ", read-only root):", "tee /usr/bin/miner");
        Sh.Result ro = held(lab, Pods.API_RO, "#!/bin/sh\n", "tee", "/usr/bin/miner");
        section("WHAT THE KERNEL ANSWERED:", ro.ok() ? "written" : "refused: " + ro.output().lines()
                .filter(l -> l.contains("Read-only")).findFirst().map(l -> l.substring(l.lastIndexOf(':') + 1).strip()).orElse("an error"));
        Set<Alert> refused = lab.newAlerts();
        section("THE ALERTS:", alerts(refused, outsideIp));
        section("WHY THE DETAIL IS A FOLDER:", "a refused open names no file, only the folder");
    }

    private void connect(Lab lab) throws Exception {
        act("FIVE. A connection the service never makes.");
        section("THE ACTION (in " + Pods.API + "):", "nc <the outside server> 4444   a server outside the cluster");
        held(lab, Pods.API, "", "nc", "-w", "2", lab.outsideIp, "4444");
        section("THE ALERTS:", alerts(lab.newAlerts(), outsideIp));
        Event e = lab.lastConnect(Pods.API, 4444);
        if (e == null) {
            section("HOW THE RECORD IS READ:", "the sensor kept no record of this connection");
            return;
        }
        List<String> how = new ArrayList<>(Audit.explain(Audit.saddr(lab.sensor.log(), e.id())));
        how.add(kv("baseline", lab.baseline.allowedIps().size() + " addresses; this one is not among them"));
        section("HOW THE RECORD IS READ:", how);
    }

    /** The key of the record each command must leave: tee writes, nc connects, sh starts. */
    private static String keyOf(String program) {
        return switch (program) {
            case "tee" -> "write";
            case "nc" -> "connect";
            default -> "exec";
        };
    }

    /** Runs a command with input, holding it until the sensor has copied a record of it (see Cluster.execWithInput). */
    private static Sh.Result held(Lab lab, String pod, String input, String... command) throws IOException, InterruptedException {
        String comm = "comm=\"" + command[0] + "\"";
        String key = "key=\"" + keyOf(command[0]) + "\"";
        long before = records(lab, comm, key);
        return lab.cluster.execWithInput(pod, input, HOLD, () -> records(lab, comm, key) > before, command);
    }

    private static long records(Lab lab, String comm, String key) throws IOException, InterruptedException {
        return lab.sensor.log().lines().filter(l -> l.contains(" CG=") && l.contains(comm) && l.contains(key)).count();
    }
}
