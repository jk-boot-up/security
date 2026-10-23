package com.jk.explore.security.meshmtls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MetricsTest {

    private static final String SAMPLE = """
            # TYPE request_total counter
            request_total{direction="inbound",target_port="8080",tls="true",client_id="web.bank.serviceaccount.identity.linkerd.cluster.local"} 3
            request_total{direction="inbound",target_port="8080",tls="true",client_id="statements.bank.serviceaccount.identity.linkerd.cluster.local"} 1
            request_total{direction="inbound",target_port="8080",tls="no_identity",no_tls_reason="no_tls_from_remote"} 2
            request_total{direction="inbound",target_port="4191",tls="no_identity",no_tls_reason="no_tls_from_remote"} 40
            request_total{direction="outbound",target_port="8080",tls="true",server_id="ledger.bank.serviceaccount.identity.linkerd.cluster.local"} 5
            response_total{direction="inbound",target_port="8080",tls="true",client_id="web.bank.serviceaccount.identity.linkerd.cluster.local"} 9
            """;

    @Test
    void sumsByTheIdentityOfTheCallerOnOnePortAndDirection() {
        Map<String, Long> m = Metrics.byIdentity(SAMPLE, "request_total", "inbound", "8080", "client_id");
        assertEquals(3L, m.get("web.bank.serviceaccount.identity.linkerd.cluster.local"));
        assertEquals(1L, m.get("statements.bank.serviceaccount.identity.linkerd.cluster.local"));
        assertEquals(2L, m.get(Metrics.NO_IDENTITY));
        assertEquals(3, m.size());
    }

    @Test
    void readsTheServerIdentityOnTheClientSide() {
        Map<String, Long> m = Metrics.byIdentity(SAMPLE, "request_total", "outbound", "8080", "server_id");
        assertEquals(Map.of("ledger.bank.serviceaccount.identity.linkerd.cluster.local", 5L), m);
    }
}
