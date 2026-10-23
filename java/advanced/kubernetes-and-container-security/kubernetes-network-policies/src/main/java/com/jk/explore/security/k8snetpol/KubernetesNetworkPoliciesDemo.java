package com.jk.explore.security.k8snetpol;

import java.util.List;

public class KubernetesNetworkPoliciesDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String line(Intent.Diff d) {
        return String.format("%d of %d pairs reachable, %d unwanted, %d missing", d.reachable(), d.total(), d.unwanted().size(), d.missing().size());
    }

    static String exit(int code) {
        return switch (code) {
            case 0 -> "answered";
            case 6 -> "the name was not found";
            case 28 -> "timed out";
            default -> "failed with " + code;
        };
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Lab lab = new Lab()) {
            out.append("--- a flat network\n");
            out.append("no policy: " + line(lab.diff()) + "\n");

            out.append("--- default deny, then one allow for each line of the design\n");
            lab.apply(Policies.denyAllIngress("shop"), Policies.denyAllIngress("pay"), Policies.denyAllIngress("ops"));
            out.append("deny all ingress in three namespaces: " + line(lab.diff()) + "\n");
            lab.applyIntended();
            Intent.Diff intended = lab.diff();
            out.append("the design's allows added: " + line(intended) + "\n");
            out.append("the measurement equals the design: " + (intended.matches() ? "yes" : "no") + "\n");

            out.append("--- three mistakes the API server accepts\n");
            lab.reset();
            lab.apply(Policies.denyAllIngress("shop"), Policies.denyAllIngress("pay"), Policies.denyAllIngress("ops"),
                    Policies.allowIngress("pay", "api", "shop", null),
                    Policies.allowIngress("pay", "db", "pay", "api"),
                    Policies.allowIngress("pay", "db", "ops", "admin"));
            Intent.Diff wide = lab.diff();
            out.append("allow from the whole shop namespace: " + line(wide) + ": " + String.join(", ", wide.unwanted()) + "\n");
            lab.reset();
            lab.apply(Policies.denyAllIngress("shop"), Policies.denyAllIngress("pay"), Policies.denyAllIngress("ops"),
                    Policies.allowIngress("pay", "apii", "shop", "web"),
                    Policies.allowIngress("pay", "db", "pay", "api"),
                    Policies.allowIngress("pay", "db", "ops", "admin"));
            Intent.Diff typoAllow = lab.diff();
            out.append("a typo in the app label of an allow: " + line(typoAllow) + ": " + String.join(", ", typoAllow.missing()) + "\n");
            lab.reset();
            lab.apply(Policies.denyIngressFor("shop", "web"), Policies.denyAllIngress("pay"), Policies.denyAllIngress("ops"),
                    Policies.allowIngress("pay", "api", "shop", "web"),
                    Policies.allowIngress("pay", "db", "pay", "api"),
                    Policies.allowIngress("pay", "db", "ops", "admin"));
            Intent.Diff typoDeny = lab.diff();
            out.append("a deny written for one app, the jobs pod forgotten: " + line(typoDeny) + ": " + String.join(", ", typoDeny.unwanted()) + "\n");

            out.append("--- egress, and the name lookup that stops with it\n");
            lab.reset();
            lab.applyIntended();
            out.append("api to a server outside the cluster, no egress policy: " + exit(lab.toOutside(Lab.API)) + "\n");
            lab.apply(Policies.denyAllEgress("pay"), Policies.allowEgress("pay", "api", "pay", "db"));
            out.append("deny egress in pay, allow api to db: " + line(lab.diff()) + "\n");
            out.append("api to the server outside: " + exit(lab.toOutside(Lab.API)) + "\n");
            out.append("api to db by its service name: " + exit(lab.dbByName(Lab.API)) + "\n");
            lab.apply(Policies.allowDns("pay"));
            out.append("api to db by its service name, DNS allowed: " + exit(lab.dbByName(Lab.API)) + "\n");

            out.append("--- the label is the credential\n");
            lab.reset();
            lab.applyIntended();
            lab.cluster.must("-n", "shop", "label", "pod", "jobs", "app=web", "--overwrite");
            Thread.sleep(6_000);
            Intent.Diff drift = lab.diff();
            out.append("the jobs pod relabelled app=web: " + line(drift) + ": " + String.join(", ", drift.unwanted()) + "\n");
        }
        out.append("--- the bill\n");
        out.append("a policy is accepted by any cluster and enforced only by its network plugin: measure, do not assume\n");
        out.append("a policy that selects nothing is valid and does nothing\n");
        out.append("whoever can set a pod's labels can choose its policies\n");
        return out.toString();
    }
}
