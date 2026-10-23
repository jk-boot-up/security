package com.jk.explore.security.k8ssecrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HttpAndManifestsTest {

    @Test
    void curlOutputIsABodyThenTheCode() {
        Http http = Http.parse("{\"auth\":{\"lease_duration\":10}}\n200");
        assertTrue(http.ok());
        assertEquals(10, http.body().path("auth").path("lease_duration").asInt());
    }

    @Test
    void anAnswerWithNoBodyStillHasACode() {
        Http http = Http.parse("403");
        assertEquals(403, http.status());
        assertFalse(http.ok());
    }

    @Test
    void theAppPodGetsAShortTokenForOneAudience() {
        String pod = Manifests.app("payments", "app", "sa");
        assertTrue(pod.contains("audience: openbao"));
        assertTrue(pod.contains("expirationSeconds: 600"));
        assertTrue(pod.contains("serviceAccountName: sa"));
    }

    @Test
    void theAuditorRoleCanOnlyList() {
        String role = Manifests.listSecretsRole("payments", "auditor");
        assertTrue(role.contains("verbs: [list]"));
        assertFalse(role.contains("get,"));
    }

    @Test
    void aTokenReviewNamesTheAudienceOnlyWhenGivenOne() {
        assertTrue(Manifests.tokenReview("t", "openbao").contains("audiences: [openbao]"));
        assertFalse(Manifests.tokenReview("t", "").contains("audiences"));
    }

    @Test
    void openBaoMayAskWhoATokenBelongsTo() {
        assertTrue(Manifests.openbao().contains("system:auth-delegator"));
    }
}
