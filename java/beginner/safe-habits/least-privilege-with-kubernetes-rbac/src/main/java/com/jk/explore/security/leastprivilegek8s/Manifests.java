package com.jk.explore.security.leastprivilegek8s;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/** The parts of Kubernetes manifests that RBAC cares about, read from YAML. */
record Manifests(List<Role> roles, List<Binding> bindings, List<Account> accounts) {

    record Rule(List<String> verbs, List<String> resources, List<String> resourceNames) {
    }

    /** A Role (namespaced) or a ClusterRole (namespace is null). */
    record Role(String namespace, String name, List<Rule> rules) {
    }

    record Subject(String namespace, String name) {
        String user() {
            return "system:serviceaccount:" + namespace + ":" + name;
        }
    }

    /** A RoleBinding (namespaced) or a ClusterRoleBinding (namespace is null). */
    record Binding(String namespace, String name, String roleKind, String roleName, List<Subject> subjects) {
    }

    record Account(String namespace, String name, boolean tokenAutomounted) {
    }

    static Manifests parse(String yaml) {
        List<Role> roles = new ArrayList<>();
        List<Binding> bindings = new ArrayList<>();
        List<Account> accounts = new ArrayList<>();
        for (Object document : new Load(LoadSettings.builder().build()).loadAllFromString(yaml)) {
            if (!(document instanceof Map<?, ?> map)) {
                continue;
            }
            Map<?, ?> metadata = (Map<?, ?>) map.get("metadata");
            String kind = (String) map.get("kind");
            String name = (String) metadata.get("name");
            String namespace = (String) metadata.get("namespace");
            switch (kind) {
                case "Role", "ClusterRole" -> roles.add(new Role("Role".equals(kind) ? namespace : null, name, rules(map.get("rules"))));
                case "RoleBinding", "ClusterRoleBinding" -> {
                    Map<?, ?> ref = (Map<?, ?>) map.get("roleRef");
                    List<Subject> subjects = new ArrayList<>();
                    for (Object each : list(map.get("subjects"))) {
                        Map<?, ?> subject = (Map<?, ?>) each;
                        if ("ServiceAccount".equals(subject.get("kind"))) {
                            subjects.add(new Subject((String) subject.get("namespace"), (String) subject.get("name")));
                        }
                    }
                    bindings.add(new Binding("RoleBinding".equals(kind) ? namespace : null, name, (String) ref.get("kind"), (String) ref.get("name"), subjects));
                }
                case "ServiceAccount" -> accounts.add(new Account(namespace, name, !Boolean.FALSE.equals(map.get("automountServiceAccountToken"))));
                default -> {
                }
            }
        }
        return new Manifests(roles, bindings, accounts);
    }

    Manifests plus(Manifests other) {
        List<Role> allRoles = new ArrayList<>(roles);
        allRoles.addAll(other.roles);
        List<Binding> allBindings = new ArrayList<>(bindings);
        allBindings.addAll(other.bindings);
        List<Account> allAccounts = new ArrayList<>(accounts);
        allAccounts.addAll(other.accounts);
        return new Manifests(allRoles, allBindings, allAccounts);
    }

    private static List<Rule> rules(Object value) {
        List<Rule> rules = new ArrayList<>();
        for (Object each : list(value)) {
            Map<?, ?> rule = (Map<?, ?>) each;
            rules.add(new Rule(strings(rule.get("verbs")), strings(rule.get("resources")), strings(rule.get("resourceNames"))));
        }
        return rules;
    }

    private static List<?> list(Object value) {
        return value == null ? List.of() : (List<?>) value;
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<>();
        for (Object each : list(value)) {
            out.add((String) each);
        }
        return out;
    }
}
