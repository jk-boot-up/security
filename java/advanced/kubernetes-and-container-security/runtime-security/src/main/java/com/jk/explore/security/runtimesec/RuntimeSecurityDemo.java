package com.jk.explore.security.runtimesec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RuntimeSecurityDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

    static String describe(Set<Alert> alerts, String outsideIp) {
        if (alerts.isEmpty()) {
            return "alerts: 0\n";
        }
        return "alerts: " + alerts.size() + "\n" + alerts.stream()
                .map(a -> "  " + a.rule() + " in " + a.pod() + ": " + a.detail().replace(outsideIp, "the outside server"))
                .collect(Collectors.joining("\n", "", "\n"));
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Path rules = Path.of("rules");
        out.append("--- Falco on this machine\n");
        out.append("the kernel of the Docker VM has BTF (type information): " + yesNo(Falco.kernelHasBtf()) + "\n");
        out.append("Falco " + Falco.IMAGE.substring(Falco.IMAGE.indexOf(':') + 1) + " with the modern eBPF driver: " + Falco.tryDriver() + "\n");
        String yaml = Files.readString(rules.resolve("bank-runtime-rules.yaml"));
        out.append("the bank's " + Falco.countRules(yaml) + " Falco rules accepted by falco --validate: " + yesNo(Falco.validates(rules, "bank-runtime-rules.yaml")) + "\n");
        out.append("so the demo applies the same three tests to Linux audit records of the same kernel\n");

        try (Lab lab = new Lab()) {
            out.append("--- the service doing its work\n");
            lab.cluster.exec(Pods.API, "wget", "-q", "-O", "/dev/null", "http://localhost:8080/");
            lab.cluster.execWithInput(Pods.API, "settled payment\n", 4_000, "tee", "/var/log/payments/audit.log");
            lab.cluster.execWithInput(Pods.API, "", 4_000, "nc", "-w", "2", "ledger-db", "5432");
            List<String> seen = List.of("exec", "write", "connect");
            Set<String> kinds = lab.seenFrom(Pods.API);
            out.append("a call to itself, a write to its log folder and a call to the ledger service; the sensor saw: "
                    + seen.stream().map(k -> k + " " + yesNo(kinds.contains(k))).collect(Collectors.joining(", ")) + "\n");
            out.append(describe(lab.newAlerts(), lab.outsideIp));

            out.append("--- a shell in the container\n");
            Sh.Result id = lab.cluster.execWithInput(Pods.API, "id -u\n", 4_000, "sh");
            out.append("someone opens a shell with kubectl exec and types id -u: it answers " + id.output().trim() + "\n");
            out.append(describe(lab.newAlerts(), lab.outsideIp));

            out.append("--- a write where the service never writes\n");
            lab.cluster.execWithInput(Pods.API, "#!/bin/sh\n", 4_000, "tee", "/usr/bin/miner");
            out.append("a program is written to /usr/bin; the log folder is written the same way and is expected\n");
            out.append(describe(lab.newAlerts(), lab.outsideIp));

            out.append("--- a connection to an address the service never calls\n");
            lab.cluster.execWithInput(Pods.API, "", 4_000, "nc", "-w", "2", lab.outsideIp, "4444");
            out.append("a connection to a server outside the cluster; the ledger service was called before and is expected\n");
            out.append(describe(lab.newAlerts(), lab.outsideIp));

            out.append("--- prevent and detect\n");
            Sh.Result ro = lab.cluster.execWithInput(Pods.API_RO, "#!/bin/sh\n", 4_000, "tee", "/usr/bin/miner");
            out.append("the same write in a pod with a read-only root file system: " + (ro.ok() ? "worked" : "refused") + "\n");
            out.append(describe(lab.newAlerts(), lab.outsideIp));
        }
        out.append("--- the bill\n");
        out.append("a detector sees what happened and prevents nothing: a read-only root and a network policy refuse, a rule tells you someone tried\n");
        out.append("this sensor looks up the container of a process after the fact, so a process that has already exited can be missed; Falco reads it in the kernel event\n");
        out.append("every rule needs a baseline of what the service does, and a new feature makes the baseline wrong\n");
        return out.toString();
    }
}
