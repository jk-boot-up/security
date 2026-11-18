package com.jk.explore.security.meshmtls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WireTest {

    /** What tcpdump -A prints for a plain HTTP call: the headers as dots and letters, then the request as text. */
    private static final String PLAIN = """
            09:41:02.118 eth0 Out IP 10.244.0.7.51234 > 10.244.0.9.8080: Flags [P.], seq 1:120, length 119
            E.....@.@..p
            ...........GET /echo?msg=ACCT-4417-transfer-9000 HTTP/1.1
            Host: payments.bank:8080
            User-Agent: curl/8.11.1
            """;

    /** The same call between two proxies: only the port and bytes that are not text. */
    private static final String MESHED = """
            09:42:10.551 eth0 Out IP 10.244.0.7.40122 > 10.244.0.9.4143: Flags [P.], seq 1:517, length 516
            E..8..@.@...........^.....z.....3...+.........E....&..q...
            """;

    @Test
    void findsTheRequestLineThatCarriesTheText() {
        assertEquals("GET /echo?msg=ACCT-4417-transfer-9000 HTTP/1.1", Wire.requestLine(PLAIN, "ACCT-4417"));
        assertFalse(Wire.proxyPort(PLAIN));
    }

    @Test
    void anEncryptedCallShowsNoRequestButTheProxyPort() {
        assertEquals("", Wire.requestLine(MESHED, "ACCT-4417"));
        assertTrue(Wire.proxyPort(MESHED));
    }

    @Test
    void aRequestWithoutTheTextDoesNotCount() {
        assertEquals("", Wire.requestLine(PLAIN, "ACCT-0000"));
    }
}
