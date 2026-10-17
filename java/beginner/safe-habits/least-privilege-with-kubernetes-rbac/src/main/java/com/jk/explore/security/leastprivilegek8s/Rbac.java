package com.jk.explore.security.leastprivilegek8s;

import com.jk.explore.security.leastprivilegek8s.Manifests.Binding;
import com.jk.explore.security.leastprivilegek8s.Manifests.Role;
import com.jk.explore.security.leastprivilegek8s.Manifests.Rule;
import com.jk.explore.security.leastprivilegek8s.Manifests.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Answers the question `kubectl auth can-i` answers, from the manifests alone: may this service account do this verb on this
 * resource in this namespace? Nothing is allowed unless a rule allows it, and there are no deny rules.
 */
final class Rbac {

    static final List<String> VERBS = List.of("get", "list", "watch", "create", "update", "patch", "delete");
    static final List<String> RESOURCES = List.of("pods", "pods/exec", "secrets", "configmaps", "services", "deployments", "roles", "rolebindings");

    private final Manifests manifests;

    Rbac(Manifests manifests) {
        this.manifests = manifests;
    }

    /** Whether the service account (as "system:serviceaccount:namespace:name") may do the verb on the resource, optionally on one named object. */
    boolean can(String user, String verb, String resource, String namespace, String objectName) {
        for (Binding binding : manifests.bindings()) {
            if (!bindsUser(binding, user) || !appliesIn(binding, namespace)) {
                continue;
            }
            Optional<Role> role = resolve(binding);
            if (role.isPresent() && role.get().rules().stream().anyMatch(rule -> allows(rule, verb, resource, objectName))) {
                return true;
            }
        }
        return false;
    }

    boolean can(String user, String verb, String resource, String namespace) {
        return can(user, verb, resource, namespace, null);
    }

    private static boolean bindsUser(Binding binding, String user) {
        return binding.subjects().stream().map(Subject::user).anyMatch(user::equals);
    }

    /** A cluster-wide binding applies everywhere; a namespaced binding applies only in its own namespace, even if it points at a cluster-wide role. */
    private static boolean appliesIn(Binding binding, String namespace) {
        return binding.namespace() == null || binding.namespace().equals(namespace);
    }

    private Optional<Role> resolve(Binding binding) {
        return manifests.roles().stream()
                .filter(role -> role.name().equals(binding.roleName()))
                .filter(role -> "ClusterRole".equals(binding.roleKind()) ? role.namespace() == null : role.namespace() != null && role.namespace().equals(binding.namespace()))
                .findFirst();
    }

    static boolean allows(Rule rule, String verb, String resource, String objectName) {
        boolean verbOk = rule.verbs().contains("*") || rule.verbs().contains(verb);
        boolean resourceOk = rule.resources().contains("*") || rule.resources().contains(resource);
        boolean nameOk = rule.resourceNames().isEmpty() || (objectName != null && rule.resourceNames().contains(objectName));
        return verbOk && resourceOk && nameOk;
    }

    /** How many of the (verb, resource) pairs in the namespace this identity may use: the blast radius if its token is stolen. */
    int blastRadius(String user, String namespace) {
        int count = 0;
        for (String resource : RESOURCES) {
            for (String verb : VERBS) {
                if (can(user, verb, resource, namespace)) {
                    count++;
                }
            }
        }
        return count;
    }

    static int universe() {
        return VERBS.size() * RESOURCES.size();
    }

    /** The well-known ways one identity can become more powerful, which this identity has in the namespace. */
    List<String> escalationPaths(String user, String namespace) {
        List<String> paths = new ArrayList<>();
        if (can(user, "get", "secrets", namespace)) {
            paths.add("read secrets");
        }
        if (can(user, "create", "pods", namespace)) {
            paths.add("create pods, which can mount any secret or service account token in the namespace");
        }
        if (can(user, "create", "pods/exec", namespace)) {
            paths.add("run commands inside pods");
        }
        if (can(user, "create", "rolebindings", namespace)) {
            paths.add("create role bindings");
        }
        if (can(user, "bind", "roles", namespace)) {
            paths.add("bind roles it does not hold");
        }
        if (can(user, "escalate", "roles", namespace)) {
            paths.add("edit roles to add permissions it does not hold");
        }
        return paths;
    }
}
