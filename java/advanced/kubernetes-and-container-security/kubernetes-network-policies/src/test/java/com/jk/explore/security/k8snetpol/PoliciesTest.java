package com.jk.explore.security.k8snetpol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PoliciesTest {

    @Test
    void denyAllSelectsEveryPodAndListsNoRules() {
        String p = Policies.denyAllIngress("pay").manifest();
        assertTrue(p.contains("podSelector: {}"));
        assertTrue(p.contains("policyTypes: [Ingress]"));
        assertFalse(p.contains("ingress:"));
    }

    @Test
    void anAllowNamesTheNamespaceAndThePodInOneElement() {
        String p = Policies.allowIngress("pay", "api", "shop", "web").manifest();
        assertTrue(p.contains("""
                    - from:
                        - namespaceSelector:
                            matchLabels: {kubernetes.io/metadata.name: shop}
                          podSelector:
                            matchLabels: {app: web}
                      ports: [{port: 8080}]
                """));
    }

    @Test
    void anAllowWithoutAPodLabelAdmitsTheWholeNamespace() {
        Policy p = Policies.allowIngress("pay", "api", "shop", null);
        assertFalse(p.manifest().contains("app: web"));
        assertEquals("allow-api-from-shop", p.name());
        assertEquals("pay app=api <- shop any pod", p.summary());
    }

    @Test
    void theDenyForOneAppSelectsOnlyThatApp() {
        assertTrue(Policies.denyIngressFor("pay", "db").manifest().contains("matchLabels: {app: db}"));
    }

    @Test
    void dnsIsAllowedOnUdpAndTcpToKubeDns() {
        String p = Policies.allowDns("pay").manifest();
        assertTrue(p.contains("k8s-app: kube-dns") && p.contains("protocol: UDP") && p.contains("protocol: TCP"));
    }

    @Test
    void everyManifestLineFitsAnOutputLine() {
        assertTrue(Policies.allowIngress("pay", "api", "shop", "web").manifest().lines().allMatch(l -> l.length() <= 62));
    }

    @Test
    void theWorkloadManifestCarriesItsLabelAndThePinnedImage() {
        assertTrue(Lab.API.manifest().contains("labels: {app: api}"));
        assertTrue(Lab.API.manifest().contains("agnhost:2.53"));
        assertEquals("pay/api", Lab.API.id());
    }
}
