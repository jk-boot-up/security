package com.jk.explore.security.k8snetpol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PoliciesTest {

    @Test
    void denyAllSelectsEveryPodAndListsNoRules() {
        String p = Policies.denyAllIngress("pay");
        assertTrue(p.contains("podSelector: {}"));
        assertTrue(p.contains("policyTypes: [Ingress]"));
        assertTrue(!p.contains("ingress:"));
    }

    @Test
    void anAllowNamesTheNamespaceAndThePodInOneElement() {
        String p = Policies.allowIngress("pay", "api", "shop", "web");
        assertTrue(p.contains("- namespaceSelector: {matchLabels: {kubernetes.io/metadata.name: shop}}\n        podSelector: {matchLabels: {app: web}}"));
    }

    @Test
    void anAllowWithoutAPodLabelAdmitsTheWholeNamespace() {
        assertTrue(!Policies.allowIngress("pay", "api", "shop", null).contains("app: web"));
    }

    @Test
    void theDenyForOneAppSelectsOnlyThatApp() {
        assertTrue(Policies.denyIngressFor("pay", "db").contains("podSelector: {matchLabels: {app: db}}"));
    }

    @Test
    void dnsIsAllowedOnUdpAndTcpToKubeDns() {
        String p = Policies.allowDns("pay");
        assertTrue(p.contains("k8s-app: kube-dns") && p.contains("protocol: UDP") && p.contains("protocol: TCP"));
    }

    @Test
    void theWorkloadManifestCarriesItsLabel() {
        assertTrue(Lab.API.manifest().contains("labels: {app: api}"));
        assertTrue(Lab.API.id().equals("pay/api"));
    }
}
