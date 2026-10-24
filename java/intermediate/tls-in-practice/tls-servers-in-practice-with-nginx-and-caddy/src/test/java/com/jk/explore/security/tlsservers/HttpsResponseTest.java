package com.jk.explore.security.tlsservers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The response view is pure: it needs no socket and no server. */
class HttpsResponseTest {

    @Test
    void headerLookupIsCaseInsensitiveOnTheName() {
        Https.Response response = new Https.Response(200, Map.of("content-type", "text/html", "strict-transport-security", "max-age=63072000"),
                "ok", "TLSv1.3", 0, List.of(), "");
        assertEquals("text/html", response.header("Content-Type"));
        assertEquals("max-age=63072000", response.header("Strict-Transport-Security"));
        assertNull(response.header("X-Absent"));
    }

    @Test
    void refusedIsTrueExactlyWhenThereIsaRefusalReason() {
        assertFalse(new Https.Response(200, Map.of(), "ok", "TLSv1.3", 0, List.of(), "").refused());
        assertTrue(new Https.Response(0, Map.of(), "", "", 0, List.of(), "certificate_unknown").refused());
    }
}
