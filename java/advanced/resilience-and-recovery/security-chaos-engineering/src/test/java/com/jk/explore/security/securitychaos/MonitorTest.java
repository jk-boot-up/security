package com.jk.explore.security.securitychaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MonitorTest {

    @Test
    void alertComesAfterExactlyThreeFailedProbesAndOnlyOnce() throws IOException {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        try (Http http = new Http(300)) {
            Monitor m = new Monitor(Map.of("fraud", "http://127.0.0.1:" + port + "/health"), http);
            m.round();
            m.round();
            assertTrue(m.alerts().isEmpty());
            m.round();
            assertEquals(List.of(new Monitor.Alert("fraud", 3)), m.alerts());
            m.round();
            m.round();
            assertEquals(1, m.alerts().size());
        }
    }

    @Test
    void healthyDependenciesRaiseNothing() throws IOException {
        try (Backends b = new Backends("127.0.0.1"); Http http = new Http(500)) {
            Monitor m = new Monitor(Map.of("audit", "http://127.0.0.1:" + b.port(Backends.AUDIT) + "/health"), http);
            for (int i = 0; i < 6; i++) {
                m.round();
            }
            assertFalse(m.alerted("audit"));
        }
    }
}
