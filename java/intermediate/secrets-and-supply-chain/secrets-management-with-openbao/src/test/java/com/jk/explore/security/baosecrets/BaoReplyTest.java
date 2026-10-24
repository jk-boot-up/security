package com.jk.explore.security.baosecrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The reply wrapper is a pure view over an already-parsed map; it needs no running OpenBao. */
class BaoReplyTest {

    @Test
    void dataReadsTheDataObject() {
        Bao.Reply reply = new Bao.Reply(200, Map.of("data", Map.of("password", "s3cret")));
        assertEquals("s3cret", reply.data().get("password"));
    }

    @Test
    void authReadsTheAuthObject() {
        Bao.Reply reply = new Bao.Reply(200, Map.of("auth", Map.of("client_token", "t.1234")));
        assertEquals("t.1234", reply.auth().get("client_token"));
    }

    @Test
    void stringIsNullWhenTheFieldIsAbsent() {
        Bao.Reply reply = new Bao.Reply(404, Map.of("errors", "x"));
        assertNull(reply.string("warnings"));
        assertEquals("x", reply.string("errors"));
    }
}
