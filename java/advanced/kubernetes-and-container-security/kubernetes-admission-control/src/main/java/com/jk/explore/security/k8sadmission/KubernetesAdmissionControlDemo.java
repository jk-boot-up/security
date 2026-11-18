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

    static final String[] DENY = {"disallow-privileged", "require-limits", "no-latest-tag", "trusted-registry", "signed-images"};

    /** The answer to a request, short: a list of more than one policy is given below the table. */
    static String answer(Verdict v) {
        if (v.admitted() || v.webhookDown() || v.policies().size() < 2) {
            return v.toString().replace("refused by ", "refused: ");
        }
        return "refused: " + v.policies().size() + " policies (below)";
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
            kyverno.apply(registry, DENY);

            out.append("ONE. Policies that refuse.\n");
            String actions = String.join(", ", new java.util.TreeSet<>(java.util.Arrays.stream(DENY).map(f -> uncheckedRead(f).action()).toList()));
            out.append("  THE POLICIES (" + actions + "; namespaces with bank/admission=enforced):\n");
            for (String f : DENY) {
                Policies.Info p = Policies.read(f);
                out.append(String.format("    %-21s%s%n", p.name(), p.message()));
            }
            String signed = host + "/bank/payments@" + payments;
            Pods.Spec good = Pods.ok(signed);
            String worst = "nginx:latest privileged, no limits";
            List<String[]> requests = List.of(
                    new String[] {"good: signed, limits", Pods.pod(good), "payments"},
                    new String[] {"privileged", Pods.pod(new Pods.Spec("p", signed, true, true, true)), "payments"},
                    new String[] {"no limits", Pods.pod(new Pods.Spec("p", signed, false, false, true)), "payments"},
                    new String[] {"signed, tagged latest", Pods.pod(Pods.ok(host + "/bank/payments:latest")), "payments"},
                    new String[] {"nginx:1.27 (another registry)", Pods.pod(Pods.ok("nginx:1.27")), "payments"},
                    new String[] {"bank/rogue (nobody signed it)", Pods.pod(Pods.ok(host + "/bank/rogue@" + rogue)), "payments"},
                    new String[] {"signed, named by the tag :1", Pods.pod(Pods.ok(host + "/bank/payments:1")), "payments"},
                    new String[] {worst, Pods.pod(new Pods.Spec("p", "nginx:latest", true, false, true)), "payments"},
                    new String[] {"a privileged Deployment", Pods.deployment(new Pods.Spec("p", signed, true, true, true)), "payments"},
                    new String[] {"privileged, in scratch (no label)", Pods.pod(new Pods.Spec("p", signed, true, true, true)), "scratch"});
            out.append("  THE REQUESTS AND THE ANSWERS (apply --dry-run=server):\n");
            Verdict worstVerdict = null;
            for (String[] r : requests) {
                Verdict v = dryRun(cluster, r[2], r[1]);
                worstVerdict = r[0].equals(worst) ? v : worstVerdict;
                out.append(String.format("    %-36s%s%n", r[0], answer(v)));
            }
            out.append("  HOW ONE REQUEST IS DECIDED (" + worst + "):\n");
            for (String f : DENY) {
                String name = Policies.read(f).name();
                out.append(String.format("    %-21s%s%n", name, worstVerdict.policies().contains(name) ? "fails" : "no objection"));
            }
            out.append("    answer: " + (worstVerdict.admitted() ? "admitted" : "refused") + " (admitted only when no policy fails)\n");
            out.append("  HOW no-latest-tag READS AN IMAGE NAME (redone in Java):\n");
            out.append(String.format("    %-31s%-6s%-6s%-8s%s%n", "image", "@", "tag", ":latest", "allowed"));
            for (String image : List.of("nginx:1.27", "nginx:latest", "registry:5000/bank/payments", "registry:5000/bank/payments:1", "bank/payments@sha256:...")) {
                TagRule t = TagRule.read(image);
                out.append(String.format("    %-31s%-6s%-6s%-8s%s%n", image, yes(t.digest()), yes(t.tag()), yes(t.latest()), yes(t.allowed())));
            }

            out.append("TWO. What was there before, and a policy in Audit.\n");
            out.append("  BEFORE THE POLICIES:\n");
            out.append("    legacy-agent, privileged, started first\n");
            out.append("  AFTER THE POLICIES:\n");
            out.append("    legacy-agent is " + cluster.must("-n", "payments", "get", "pod", "legacy-agent", "-o", "jsonpath={.status.phase}") + " (admission sees only new requests)\n");
            kyverno.apply(registry, "readonly-rootfs");
            Policies.Info ro = Policies.read("readonly-rootfs");
            out.append("  A NEW POLICY IN AUDIT: " + ro.name() + " (" + ro.action() + ")\n");
            Sh.Result ledger = cluster.apply("payments", Pods.pod(new Pods.Spec("ledger", signed, false, true, false)));
            out.append("    ledger, a writable root filesystem: " + Verdict.of(ledger) + "\n");
            List<Kyverno.Entry> report = kyverno.reports("payments", "readonly-rootfs", "ledger");
            out.append("  THE POLICY REPORT (what breaks a policy, refused or not):\n");
            out.append(String.format("    %-20s%-14s%s%n", "readonly-rootfs", "ledger", result(report, "readonly-rootfs", "ledger")));
            out.append(String.format("    %-20s%-14s%s%n", "disallow-privileged", "legacy-agent", result(report, "disallow-privileged", "legacy-agent")));
            kyverno.setAction("readonly-rootfs", "Deny");
            out.append("  AFTER THE SWITCH TO Deny:\n");
            out.append("    ledger2, a writable root filesystem: " + dryRun(cluster, "payments", Pods.pod(new Pods.Spec("ledger2", signed, false, true, false))) + "\n");

            out.append("THREE. A policy that changes the request.\n");
            kyverno.apply(registry, "default-no-escalation");
            String tagged = Pods.pod(Pods.ok(host + "/bank/payments:1"));
            out.append("  BEFORE THE MUTATION (what the team sent):\n");
            tagged.lines().filter(l -> l.contains("image:")).forEach(l -> out.append("    " + l.strip().replace(host, "<registry>") + "\n"));
            String sent = tagged.lines().filter(l -> l.contains("securityContext:")).findFirst().orElseThrow();
            out.append("    securityContext:\n");
            fields(sent.substring(sent.indexOf('{'))).forEach(f -> out.append("      " + f + "\n"));
            String sc = cluster.must("-n", "payments", "apply", "--dry-run=server", "-o", "jsonpath={.spec.containers[0].securityContext}", "-f", file(tagged));
            String image = cluster.must("-n", "payments", "apply", "--dry-run=server", "-o", "jsonpath={.spec.containers[0].image}", "-f", file(tagged));
            out.append("  AFTER THE MUTATION (what the API server would store):\n");
            out.append("    image: " + image.replace(host, "<registry>").replace(payments, "<digest>") + "\n");
            out.append("    securityContext:\n");
            fields(sc).forEach(f -> out.append("      " + f + "\n"));
            out.append("    <digest> is the digest the bank signed: " + yes(image.endsWith("@" + payments)) + "\n");
            out.append("  A PRIVILEGED POD (mutation first, then validation):\n");
            out.append("    " + answer(dryRun(cluster, "payments", Pods.pod(new Pods.Spec("p", signed, true, true, true)))) + "\n");

            out.append("FOUR. Exceptions.\n");
            Pods.Spec agent = new Pods.Spec("node-agent", signed, true, true, true);
            out.append("  THE EXCEPTION (node-agent may be privileged):\n");
            out.append("    policy       disallow-privileged\n");
            out.append("    applies to   a pod named node-agent\n");
            out.append("    read from    the kyverno namespace only\n");
            out.append("  THE REQUESTS AND THE ANSWERS:\n");
            kyverno.applyException("kyverno", "2099-01-01T00:00:00Z");
            out.append(String.format("    %-36s%s%n", "node-agent, exception until 2099", answer(dryRun(cluster, "payments", Pods.pod(agent)))));
            out.append(String.format("    %-36s%s%n", "another-agent, same spec", answer(dryRun(cluster, "payments", Pods.pod(agent.withName("another-agent"))))));
            cluster.must("-n", "kyverno", "delete", "policyexceptions.policies.kyverno.io", "node-agent");
            kyverno.applyException("payments", "2099-01-01T00:00:00Z");
            out.append(String.format("    %-36s%s%n", "node-agent, exception in payments", answer(dryRun(cluster, "payments", Pods.pod(agent)))));
            cluster.must("-n", "payments", "delete", "policyexceptions.policies.kyverno.io", "node-agent");
            kyverno.applyException("kyverno", "2020-01-01T00:00:00Z");
            out.append(String.format("    %-36s%s%n", "node-agent, exception expired 2020", answer(dryRun(cluster, "payments", Pods.pod(agent)))));

            out.append("FIVE. When the admission controller is down.\n");
            Pods.Spec privileged = new Pods.Spec("p", signed, true, true, true);
            out.append("  BEFORE (Kyverno running):\n");
            out.append("    a privileged pod: " + answer(dryRun(cluster, "payments", Pods.pod(privileged))) + "\n");
            kyverno.scale(0);
            out.append("  KYVERNO STOPPED:\n");
            out.append("    policy webhooks left in the API server: " + cluster.must("get", "validatingwebhookconfiguration", "-o", "name").lines().filter(l -> l.contains("resource-validating")).count() + "\n");
            out.append("    a good pod: " + answer(dryRun(cluster, "payments", Pods.pod(good))) + "\n");
            out.append("    a privileged pod: " + answer(dryRun(cluster, "payments", Pods.pod(privileged))) + "\n");
            kyverno.scale(1);
            out.append("  AFTER IT IS BACK:\n");
            out.append("    a good pod: " + answer(dryRun(cluster, "payments", Pods.pod(good))) + "\n");
            out.append("    a privileged pod: " + answer(dryRun(cluster, "payments", Pods.pod(privileged))) + "\n");
        }
        return out.toString();
    }

    /** The fields of a flow map, written as YAML ({a: true}) or as JSON ({"a":true}), as "a: true" lines. */
    static List<String> fields(String map) {
        String inner = map.strip().replaceAll("^\\{|}$", "").replace("\"", "");
        return java.util.Arrays.stream(inner.split(",")).map(f -> f.strip().replaceFirst(":\\s*", ": ")).toList();
    }

    private static Policies.Info uncheckedRead(String file) {
        try {
            return Policies.read(file);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
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
