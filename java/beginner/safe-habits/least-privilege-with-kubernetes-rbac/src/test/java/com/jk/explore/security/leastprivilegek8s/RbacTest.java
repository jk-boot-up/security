package com.jk.explore.security.leastprivilegek8s;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jk.explore.security.leastprivilegek8s.Manifests.Rule;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RbacTest {

    private Rbac rbac;

    @BeforeEach
    void load() throws Exception {
        rbac = new Rbac(LeastPrivilegeK8sDemo.load());
    }

    private static String user(String name) {
        return LeastPrivilegeK8sDemo.user(name);
    }

    @Test
    void nothingIsAllowedWithoutABinding() {
        assertFalse(rbac.can(user("nobody"), "get", "pods", "shop"));
        assertEquals(0, rbac.blastRadius(user("nobody"), "shop"));
    }

    @Test
    void clusterAdminReachesEverythingEverywhere() {
        assertEquals(Rbac.universe(), rbac.blastRadius(user("nightly-export-admin"), "shop"));
        assertTrue(rbac.can(user("nightly-export-admin"), "delete", "secrets", "warehouse"));
    }

    @Test
    void exporterReachesTwoPairsAndOneNamedConfigMap() {
        assertEquals(2, rbac.blastRadius(user("exporter"), "shop"));
        assertTrue(rbac.can(user("exporter"), "get", "configmaps", "shop", "export-settings"));
        assertFalse(rbac.can(user("exporter"), "get", "configmaps", "shop", "payment-keys"));
        assertFalse(rbac.can(user("exporter"), "get", "configmaps", "shop"));
        assertFalse(rbac.can(user("exporter"), "list", "pods", "warehouse"));
    }

    @Test
    void wildcardResourcesIncludeSecrets() {
        assertTrue(rbac.can(user("reporter"), "get", "secrets", "shop"));
        assertFalse(rbac.can(user("reporter"), "delete", "pods", "shop"));
        assertEquals(16, rbac.blastRadius(user("reporter"), "shop"));
    }

    @Test
    void namespacedBindingOfAClusterRoleStaysInItsNamespace() {
        assertTrue(rbac.can(user("support-agent"), "list", "pods", "shop"));
        assertFalse(rbac.can(user("support-agent"), "list", "pods", "warehouse"));
        assertFalse(rbac.can(user("support-agent"), "delete", "pods", "shop"));
    }

    @Test
    void escalationPathsAreFoundForTheDeployerOnly() {
        assertEquals(4, rbac.escalationPaths(user("ci-deployer"), "shop").size());
        assertTrue(rbac.escalationPaths(user("exporter"), "shop").isEmpty());
        assertTrue(rbac.escalationPaths(user("reporter"), "shop").contains("read secrets"));
    }

    @Test
    void resourceNamesNeedAName() {
        Rule rule = new Rule(List.of("get"), List.of("configmaps"), List.of("a"));
        assertTrue(Rbac.allows(rule, "get", "configmaps", "a"));
        assertFalse(Rbac.allows(rule, "get", "configmaps", "b"));
        assertFalse(Rbac.allows(rule, "get", "configmaps", null));
        assertFalse(Rbac.allows(rule, "list", "configmaps", "a"));
    }

    @Test
    void aSubresourceIsNotImpliedByItsParent() {
        Rule rule = new Rule(List.of("create"), List.of("pods"), List.of());
        assertTrue(Rbac.allows(rule, "create", "pods", null));
        assertFalse(Rbac.allows(rule, "create", "pods/exec", null));
    }
}
