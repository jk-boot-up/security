package com.jk.explore.security.leastprivilegek8s;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ManifestsAndLinterTest {

    private static final String YAML = """
            apiVersion: v1
            kind: ServiceAccount
            metadata: {name: a, namespace: n}
            automountServiceAccountToken: false
            ---
            apiVersion: rbac.authorization.k8s.io/v1
            kind: ClusterRole
            metadata: {name: wide}
            rules:
              - {apiGroups: ["*"], resources: ["*"], verbs: ["*"]}
            ---
            apiVersion: rbac.authorization.k8s.io/v1
            kind: ClusterRoleBinding
            metadata: {name: b}
            subjects:
              - {kind: ServiceAccount, name: a, namespace: n}
              - {kind: User, name: someone}
            roleRef: {apiGroup: rbac.authorization.k8s.io, kind: ClusterRole, name: wide}
            """;

    @Test
    void parsesAccountsRolesAndBindings() {
        Manifests m = Manifests.parse(YAML);
        assertEquals(1, m.accounts().size());
        assertTrue(!m.accounts().get(0).tokenAutomounted());
        assertEquals(1, m.roles().size());
        assertEquals(null, m.roles().get(0).namespace());
        assertEquals(1, m.bindings().get(0).subjects().size());
        assertEquals("system:serviceaccount:n:a", m.bindings().get(0).subjects().get(0).user());
    }

    @Test
    void emptyDocumentsAreIgnored() {
        assertEquals(0, Manifests.parse("---\n---\n").roles().size());
    }

    @Test
    void lintFlagsWildcardsAndEscalationVerbs() {
        List<String> found = Linter.check(Manifests.parse(YAML)).stream().map(Linter.Finding::toString).toList();
        assertTrue(found.contains("wide: wildcard verbs"));
        assertTrue(found.contains("wide: wildcard resources"));
        assertEquals(2, found.size());
    }

    @Test
    void lintFlagsClusterAdminBindingsAndAutomountedTokens() throws Exception {
        List<String> found = Linter.check(LeastPrivilegeK8sDemo.load()).stream().map(Linter.Finding::toString).toList();
        assertTrue(found.contains("nightly-export-is-admin: binds cluster-admin"));
        assertTrue(found.contains("default: token is mounted into every pod"));
        assertTrue(found.contains("deployer: escalation verb bind"));
        assertEquals(6, found.size());
    }
}
