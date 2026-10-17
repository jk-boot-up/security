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

    private static void act1ClusterAdmin(StringBuilder out, Rbac rbac) {
        line(out, "ONE. The nightly export job that was given cluster-admin.");
        line(out, "  the shop's namespace has " + Rbac.universe() + " things a token could be used for: " + Rbac.VERBS.size() + " verbs on " + Rbac.RESOURCES.size() + " kinds of object");
        int admin = rbac.blastRadius(user("nightly-export-admin"), NAMESPACE);
        int exporter = rbac.blastRadius(user("exporter"), NAMESPACE);
        line(out, "  if the nightly-export-admin token is stolen it reaches " + admin + " of " + Rbac.universe() + "  <- THE WHOLE NAMESPACE, AND THE WHOLE CLUSTER");
        line(out, "  if the exporter token is stolen it reaches " + exporter + " of " + Rbac.universe() + ": get and list pods (one named config map is also readable, and is not counted)");
        line(out, "  the exporter can read the config map export-settings: " + rbac.can(user("exporter"), "get", "configmaps", NAMESPACE, "export-settings"));
        line(out, "  the exporter can read another config map, payment-keys: " + rbac.can(user("exporter"), "get", "configmaps", NAMESPACE, "payment-keys"));
        line(out, "  the exporter can read secrets: " + rbac.can(user("exporter"), "get", "secrets", NAMESPACE));
    }

    private static void act2WildcardTrap(StringBuilder out, Rbac rbac) {
        line(out, "TWO. The wildcard trap: \"read everything\".");
        String reporter = user("reporter");
        line(out, "  the reporter's role says get and list on all resources, which sounds read-only and harmless");
        line(out, "  the reporter can read secrets: " + rbac.can(reporter, "get", "secrets", NAMESPACE) + "  <- EVERY PASSWORD AND KEY IN THE NAMESPACE");
        line(out, "  the reporter can change anything: " + rbac.can(reporter, "update", "deployments", NAMESPACE));
        line(out, "  the reporter reaches " + rbac.blastRadius(reporter, NAMESPACE) + " of " + Rbac.universe() + ", but \"everything\" includes things that did not exist when the role was written");
        line(out, "  a rule that names its resources keeps meaning what it said, and a rule with resourceNames names the objects as well");
    }

    private static void act3Escalation(StringBuilder out, Rbac rbac) {
        line(out, "THREE. Ways to become more powerful than you were given.");
        for (String name : List.of("exporter", "ci-deployer")) {
            List<String> paths = rbac.escalationPaths(user(name), NAMESPACE);
            line(out, "  " + name + ": " + paths.size() + " escalation paths");
            for (String path : paths) {
                line(out, "    can " + path);
            }
        }
        line(out, "  the deploy pipeline can read secrets directly: " + rbac.can(user("ci-deployer"), "get", "secrets", NAMESPACE)
                + ", and it can create a pod that mounts them: " + rbac.can(user("ci-deployer"), "create", "pods", NAMESPACE));
        line(out, "  so \"create pods\" is as powerful as \"read secrets\", and \"bind\" or \"escalate\" is as powerful as owning the namespace");
    }

    private static void act4Scope(StringBuilder out, Rbac rbac) {
        line(out, "FOUR. A cluster-wide role through a namespaced binding.");
        String agent = user("support-agent");
        line(out, "  the role shop-viewer is a ClusterRole, and the support agent holds it through a RoleBinding in the shop namespace");
        line(out, "  the support agent can list pods in shop: " + rbac.can(agent, "list", "pods", "shop"));
        line(out, "  the support agent can list pods in warehouse: " + rbac.can(agent, "list", "pods", "warehouse"));
        line(out, "  the support agent can delete pods in shop: " + rbac.can(agent, "delete", "pods", "shop"));
        String admin = user("nightly-export-admin");
        line(out, "  the cluster-admin job can list pods in warehouse: " + rbac.can(admin, "list", "pods", "warehouse") + "  <- A CLUSTER BINDING IGNORES NAMESPACES");
        line(out, "  the binding decides the reach, not the role: a role can be shared, a binding is where the scope is set");
    }

    private static void act5Linter(StringBuilder out, Manifests manifests) {
        line(out, "FIVE. A linter for the habits that cause incidents.");
        List<Linter.Finding> findings = Linter.check(manifests);
        line(out, "  findings in the shop manifests: " + findings.size());
        for (Linter.Finding finding : findings) {
            line(out, "    " + finding);
        }
        line(out, "  a linter finds wildcards and cluster-admin, not a role that is too wide but spelled out in full");
        line(out, "  ask the cluster too: kubectl auth can-i --list --as=system:serviceaccount:shop:exporter -n shop");
    }
}
