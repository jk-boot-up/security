package com.jk.explore.security.meshmtls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PoliciesTest {

    @Test
    void identityIsServiceAccountNamespaceAndTrustDomain() {
        assertEquals("payments.bank.serviceaccount.identity.linkerd.cluster.local", Lab.PAYMENTS.identity());
    }

    @Test
    void aServiceAccountOfAnotherNamespaceIsAnotherIdentity() {
        assertFalse(Lab.PAYMENTS.identity().equals(new Service("shadow", "payments").identity()));
    }

    @Test
    void aServerWithoutCallersHasNoAuthorization() {
        String yaml = Policies.forService(Lab.WEB, List.of());
        assertTrue(yaml.contains("kind: Server"));
        assertFalse(yaml.contains("AuthorizationPolicy"));
    }

    @Test
    void theLedgerPolicyNamesOnlyPaymentsAndRequiresMeshTls() {
        String yaml = Policies.forService(Lab.LEDGER, List.of(Lab.PAYMENTS));
        assertTrue(yaml.contains("identities: [\"payments.bank.serviceaccount.identity.linkerd.cluster.local\"]"));
        assertTrue(yaml.contains("kind: MeshTLSAuthentication"));
        assertFalse(yaml.contains("web.bank"));
        assertTrue(yaml.contains("name: ledger-http"));
    }

    @Test
    void aWorkloadThatRunsAsAnotherAccountDoesNotCreateIt() {
        Service impostor = new Service("bank", "impostor", "payments");
        assertFalse(impostor.manifest().contains("kind: ServiceAccount"));
        assertTrue(impostor.manifest().contains("serviceAccountName: payments"));
        assertTrue(Lab.WEB.manifest().contains("kind: ServiceAccount"));
    }

    @Test
    void theReleaseAssetNameFollowsTheMachine() {
        assertEquals("linkerd2-cli-" + Linkerd.RELEASE + "-darwin-arm64", Linkerd.assetName("Mac OS X", "aarch64"));
        assertEquals("linkerd2-cli-" + Linkerd.RELEASE + "-linux-amd64", Linkerd.assetName("Linux", "amd64"));
    }
}
