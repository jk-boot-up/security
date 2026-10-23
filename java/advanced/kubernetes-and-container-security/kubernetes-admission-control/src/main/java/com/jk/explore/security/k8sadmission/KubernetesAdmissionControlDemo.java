package com.jk.explore.security.k8sadmission;

import java.util.List;

public class KubernetesAdmissionControlDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static final String PAUSE = "registry.k8s.io/pause:3.10";

    static Verdict dryRun(Cluster c, String namespace, String manifest) throws Exception {
        return Verdict.of(c.apply(namespace, manifest, "--dry-run=server"));
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Cluster cluster = new Cluster(); Registry registry = new Registry()) {
            String host = registry.clusterHost();
            String payments = registry.push("bank/payments", "payments");
            registry.sign("bank/payments", payments);
            String rogue = registry.push("bank/rogue", "rogue");
            cluster.namespace("payments", true);
            cluster.namespace("scratch", false);

            // a pod that was running before there was any policy
            cluster.must("-n", "payments", "run", "legacy-agent", "--image=" + PAUSE, "--overrides={\"spec\":{\"containers\":[{\"name\":\"legacy-agent\",\"image\":\"" + PAUSE + "\",\"securityContext\":{\"privileged\":true}}]}}");
            cluster.must("-n", "payments", "wait", "--for=condition=Ready", "pod/legacy-agent", "--timeout=120s");

            Kyverno kyverno = new Kyverno(cluster);
            kyverno.apply(registry, "disallow-privileged", "require-limits", "no-latest-tag", "trusted-registry", "signed-images");

            out.append("--- policies that refuse\n");
            String signed = host + "/bank/payments@" + payments;
            Pods.Spec good = Pods.ok(signed);
            List<String[]> requests = List.of(
                    new String[] {"a signed image, limits, not privileged", Pods.pod(good), "payments"},
                    new String[] {"privileged", Pods.pod(new Pods.Spec("p", signed, true, true, true)), "payments"},
                    new String[] {"no limits", Pods.pod(new Pods.Spec("p", signed, false, false, true)), "payments"},
                    new String[] {"signed image tagged latest", Pods.pod(Pods.ok(host + "/bank/payments:latest")), "payments"},
                    new String[] {"image from another registry", Pods.pod(Pods.ok("nginx:1.27")), "payments"},
                    new String[] {"image nobody signed", Pods.pod(Pods.ok(host + "/bank/rogue@" + rogue)), "payments"},
                    new String[] {"signed image named by a tag", Pods.pod(Pods.ok(host + "/bank/payments:1")), "payments"},
                    new String[] {"latest, privileged, no limits", Pods.pod(new Pods.Spec("p", "nginx:latest", true, false, true)), "payments"},
                    new String[] {"a privileged Deployment", Pods.deployment(new Pods.Spec("p", signed, true, true, true)), "payments"},
                    new String[] {"privileged, in a namespace without the label", Pods.pod(new Pods.Spec("p", signed, true, true, true)), "scratch"});
            for (String[] r : requests) {
                out.append(String.format("%-46s %s%n", r[0], dryRun(cluster, r[2], r[1])));
            }

            out.append("--- what was there before, and the audit mode\n");
            out.append("legacy-agent (privileged, started before the policies) is " + cluster.must("-n", "payments", "get", "pod", "legacy-agent", "-o", "jsonpath={.status.phase}") + "\n");
            kyverno.apply(registry, "readonly-rootfs");
            Sh.Result ledger = cluster.apply("payments", Pods.pod(new Pods.Spec("ledger", signed, false, true, false)));
            out.append("ledger (writable root filesystem) with the policy in Audit: " + Verdict.of(ledger) + "\n");
            List<Kyverno.Entry> report = kyverno.reports("payments", "readonly-rootfs", "ledger");
            out.append("the report says: readonly-rootfs on ledger " + result(report, "readonly-rootfs", "ledger") + "\n");
            out.append("the report says: disallow-privileged on legacy-agent " + result(report, "disallow-privileged", "legacy-agent") + "\n");
            kyverno.setAction("readonly-rootfs", "Deny");
            out.append("a new pod with a writable root filesystem, policy now in Deny: " + dryRun(cluster, "payments", Pods.pod(new Pods.Spec("ledger2", signed, false, true, false))) + "\n");

            out.append("--- mutation\n");
            kyverno.apply(registry, "default-no-escalation");
            String sc = cluster.must("-n", "payments", "apply", "--dry-run=server", "-o", "jsonpath={.spec.containers[0].securityContext}", "-f", file(Pods.pod(good)));
            out.append("a pod that said nothing about privilege escalation: allowPrivilegeEscalation false added=" + yes(sc.contains("\"allowPrivilegeEscalation\":false")) + ", its own settings kept=" + yes(sc.contains("readOnlyRootFilesystem")) + "\n");
            String image = cluster.must("-n", "payments", "apply", "--dry-run=server", "-o", "jsonpath={.spec.containers[0].image}", "-f", file(Pods.pod(Pods.ok(host + "/bank/payments:1"))));
            out.append("the tag :1 was replaced by the digest of the signed image: " + yes(image.endsWith("@" + payments)) + "\n");
            out.append("a privileged pod after the mutation: " + dryRun(cluster, "payments", Pods.pod(new Pods.Spec("p", signed, true, true, true))) + "\n");

            out.append("--- exceptions\n");
            Pods.Spec agent = new Pods.Spec("node-agent", signed, true, true, true);
            kyverno.applyException("kyverno", "2099-01-01T00:00:00Z");
            out.append("node-agent, with an exception until 2099: " + dryRun(cluster, "payments", Pods.pod(agent)) + "\n");
            out.append("another-agent, same spec, no exception: " + dryRun(cluster, "payments", Pods.pod(agent.withName("another-agent"))) + "\n");
            cluster.must("-n", "kyverno", "delete", "policyexceptions.policies.kyverno.io", "node-agent");
            kyverno.applyException("payments", "2099-01-01T00:00:00Z");
            out.append("node-agent, the exception written in the team's own namespace: " + dryRun(cluster, "payments", Pods.pod(agent)) + "\n");
            cluster.must("-n", "payments", "delete", "policyexceptions.policies.kyverno.io", "node-agent");
            kyverno.applyException("kyverno", "2020-01-01T00:00:00Z");
            out.append("node-agent, the exception expired in 2020: " + dryRun(cluster, "payments", Pods.pod(agent)) + "\n");

            out.append("--- when the controller is down\n");
            kyverno.scale(0);
            out.append("policy webhooks left in the API server: " + cluster.must("get", "validatingwebhookconfiguration", "-o", "name").lines().filter(l -> l.contains("resource-validating")).count() + "\n");
            out.append("a good pod, the admission controller stopped: " + dryRun(cluster, "payments", Pods.pod(good)) + "\n");
            kyverno.scale(1);
            out.append("the same pod after it is back: " + dryRun(cluster, "payments", Pods.pod(good)) + "\n");
        }
        out.append("--- the bill\n");
        out.append("admission sees requests, not what already runs; a namespace without the label is not covered\n");
        out.append("a stopped Kyverno removes its webhooks, and with none left the API server admits everything\n");
        return out.toString();
    }

    static String result(List<Kyverno.Entry> entries, String policy, String resource) {
        return entries.stream().filter(e -> e.policy().equals(policy) && e.resource().equals(resource)).map(Kyverno.Entry::result).findFirst().orElse("(none)");
    }

    static String file(String text) throws java.io.IOException {
        java.nio.file.Path f = java.nio.file.Files.createTempFile("m", ".yaml");
        java.nio.file.Files.writeString(f, text);
        return f.toString();
    }
}
