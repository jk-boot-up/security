package com.jk.explore.security.leastprivilegek8s;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Runs the online store story in five numbered acts, reading the real manifests in infra/rbac and printing exact, repeatable results. */
public class LeastPrivilegeK8sDemo {

    static final String NAMESPACE = "shop";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String user(String name) {
        return "system:serviceaccount:" + NAMESPACE + ":" + name;
    }

    static Manifests load() throws IOException {
        Manifests shop = Manifests.parse(Files.readString(Path.of("infra", "rbac", "shop.yaml")));
        try (InputStream in = LeastPrivilegeK8sDemo.class.getResourceAsStream("/builtin-clusterroles.yaml")) {
            return shop.plus(Manifests.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        }
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException {
        Rbac rbac = new Rbac(load());
        StringBuilder out = new StringBuilder();
        act1ClusterAdmin(out, rbac);
        act2WildcardTrap(out, rbac);
        act3Escalation(out, rbac);
        act4Scope(out, rbac);
        act5Linter(out, load());
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    /** One rule as a person would read it: "get, list on pods named export-settings". */
    private static String rule(Manifests.Rule rule) {
        String names = rule.resourceNames().isEmpty() ? "" : " named " + String.join(", ", rule.resourceNames());
        return String.join(", ", rule.verbs()) + " on " + String.join(", ", rule.resources()) + names;
    }

    private static void rules(StringBuilder out, Rbac rbac, String account) {
        for (Manifests.Role role : rbac.rolesOf(user(account))) {
            int n = 1;
            for (Manifests.Rule r : role.rules()) {
                line(out, "    rule " + n++ + "  " + rule(r));
            }
        }
    }

    private static void act1ClusterAdmin(StringBuilder out, Rbac rbac) {
        line(out, "ONE. The nightly export job that was given cluster-admin.");
        line(out, "  THE NAMESPACE shop (what a token could be used for):");
        line(out, "    " + Rbac.VERBS.size() + " verbs      " + String.join(" ", Rbac.VERBS));
        line(out, "    " + Rbac.RESOURCES.size() + " resources  " + String.join(" ", Rbac.RESOURCES.subList(0, 5)));
        line(out, "                 " + String.join(" ", Rbac.RESOURCES.subList(5, Rbac.RESOURCES.size())));
        line(out, "    " + Rbac.VERBS.size() + " x " + Rbac.RESOURCES.size() + " = " + Rbac.universe() + " things");
        int admin = rbac.blastRadius(user("nightly-export-admin"), NAMESPACE);
        int exporter = rbac.blastRadius(user("exporter"), NAMESPACE);
        line(out, "  BEFORE THE FIX (nightly-export-admin, bound to cluster-admin):");
        line(out, "    a stolen token reaches " + admin + " of " + Rbac.universe() + "  <- THE WHOLE CLUSTER");
        line(out, "  AFTER THE FIX (exporter, with the role order-exporter):");
        rules(out, rbac, "exporter");
        line(out, "    a stolen token reaches " + exporter + " of " + Rbac.universe() + ", plus one named config map");
        line(out, "  THE CHECKS (can the exporter ...?):");
        line(out, "    get configmap export-settings  " + rbac.can(user("exporter"), "get", "configmaps", NAMESPACE, "export-settings"));
        line(out, "    get configmap payment-keys     " + rbac.can(user("exporter"), "get", "configmaps", NAMESPACE, "payment-keys"));
        line(out, "    get secrets                    " + rbac.can(user("exporter"), "get", "secrets", NAMESPACE));
    }

    private static void act2WildcardTrap(StringBuilder out, Rbac rbac) {
        line(out, "TWO. The wildcard trap: \"read everything\".");
        String reporter = user("reporter");
        line(out, "  THE ROLE AS WRITTEN (report-reader, held by reporter):");
        rules(out, rbac, "reporter");
        line(out, "    \"read everything\": it sounds read-only and harmless");
        line(out, "  WHAT IT REALLY ALLOWS:");
        line(out, "    get secrets         " + rbac.can(reporter, "get", "secrets", NAMESPACE) + "  <- EVERY PASSWORD AND KEY");
        line(out, "    update deployments  " + rbac.can(reporter, "update", "deployments", NAMESPACE));
        int verbs = rbac.rolesOf(reporter).get(0).rules().get(0).verbs().size();
        line(out, "    reaches " + rbac.blastRadius(reporter, NAMESPACE) + " of " + Rbac.universe() + ": " + verbs + " verbs x "
                + Rbac.RESOURCES.size() + " resources");
        line(out, "  * also covers kinds added after the role was written;");
        line(out, "  name the resources, and use resourceNames for one object");
    }

    private static void act3Escalation(StringBuilder out, Rbac rbac) {
        line(out, "THREE. Ways to become more powerful than you were given.");
        line(out, "  THE ROLE (deployer, held by ci-deployer):");
        rules(out, rbac, "ci-deployer");
        List<String> none = rbac.escalationPaths(user("exporter"), NAMESPACE);
        List<String> paths = rbac.escalationPaths(user("ci-deployer"), NAMESPACE);
        line(out, "  THE ESCALATION PATHS (exporter " + none.size() + ", ci-deployer " + paths.size() + "):");
        for (String path : paths) {
            line(out, "    " + path);
        }
        line(out, "  ci-deployer: get secrets " + rbac.can(user("ci-deployer"), "get", "secrets", NAMESPACE)
                + ", but create pods " + rbac.can(user("ci-deployer"), "create", "pods", NAMESPACE));
        line(out, "  a pod can mount the secrets  <- CREATE PODS = READ SECRETS");
        line(out, "  and bind or escalate = owning the namespace");
    }

    private static void act4Scope(StringBuilder out, Rbac rbac) {
        line(out, "FOUR. A cluster-wide role through a namespaced binding.");
        String agent = user("support-agent");
        line(out, "  THE ROLE AND THE BINDING:");
        line(out, "    shop-viewer is a ClusterRole: it has no namespace");
        rules(out, rbac, "support-agent");
        line(out, "    support-agent holds it by a RoleBinding in shop");
        line(out, "  THE CHECKS (can support-agent ...?):");
        line(out, "    list pods in shop        " + rbac.can(agent, "list", "pods", "shop"));
        line(out, "    list pods in warehouse   " + rbac.can(agent, "list", "pods", "warehouse"));
        line(out, "    delete pods in shop      " + rbac.can(agent, "delete", "pods", "shop"));
        String admin = user("nightly-export-admin");
        line(out, "  BY CONTRAST, A ClusterRoleBinding (nightly-export-admin):");
        line(out, "    list pods in warehouse   " + rbac.can(admin, "list", "pods", "warehouse")
                + "  <- IGNORES NAMESPACES");
        line(out, "  the binding sets the scope, not the role");
    }

    private static void act5Linter(StringBuilder out, Manifests manifests) {
        line(out, "FIVE. A linter for the habits that cause incidents.");
        line(out, "  THE MANIFESTS (infra/rbac/shop.yaml):");
        line(out, "    " + manifests.roles().size() + " roles, " + manifests.bindings().size() + " bindings, "
                + manifests.accounts().size() + " service accounts");
        List<Linter.Finding> findings = Linter.check(manifests);
        line(out, "  THE FINDINGS (" + findings.size() + "):");
        for (Linter.Finding finding : findings) {
            line(out, "    " + pad(finding.object(), 25) + finding.problem());
        }
        line(out, "  a linter finds wildcards and cluster-admin,");
        line(out, "  not a role that is too wide but spelled out in full");
        line(out, "  ask the cluster too:");
        line(out, "    kubectl auth can-i --list -n shop \\");
        line(out, "      --as=system:serviceaccount:shop:exporter");
    }
}
