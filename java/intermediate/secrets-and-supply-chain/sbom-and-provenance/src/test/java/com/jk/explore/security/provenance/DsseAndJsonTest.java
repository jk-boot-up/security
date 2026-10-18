package com.jk.explore.security.provenance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DsseAndJsonTest {

    @Test
    void paeMatchesTheDsseSpecificationExample() {
        // The DSSE protocol document's example: PAE("http://example.com/HelloWorld", "hello world")
        byte[] pae = Dsse.pae("http://example.com/HelloWorld", "hello world".getBytes(StandardCharsets.UTF_8));
        assertEquals("DSSEv1 29 http://example.com/HelloWorld 11 hello world", new String(pae, StandardCharsets.UTF_8));
    }

    @Test
    void paeCountsBytesNotCharacters() {
        byte[] pae = Dsse.pae("t", "é".getBytes(StandardCharsets.UTF_8));
        assertEquals("DSSEv1 1 t 2 é", new String(pae, StandardCharsets.UTF_8));
    }

    @Test
    void anEnvelopeOpensWithTheTrustedKeyOnly() throws Exception {
        Builder ci = Builder.demo("ci", "ci-key", 7);
        Builder other = Builder.demo("other", "ci-key", 8);
        byte[] payload = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        String envelope = Dsse.envelope(payload, ci.keyId(), ci.key().getPrivate());
        assertArrayEquals(payload, Dsse.open(envelope, Map.of("ci-key", ci.key().getPublic())).payload());
        Dsse.BadEnvelopeException wrong = assertThrows(Dsse.BadEnvelopeException.class,
                () -> Dsse.open(envelope, Map.of("ci-key", other.key().getPublic())));
        assertEquals("the signature does not match the statement", wrong.getMessage());
        Dsse.BadEnvelopeException unknown = assertThrows(Dsse.BadEnvelopeException.class,
                () -> Dsse.open(envelope, Map.of("someone-else", ci.key().getPublic())));
        assertEquals("no signature from a trusted builder", unknown.getMessage());
    }

    @Test
    void badEnvelopesAreRefused() {
        Map<String, java.security.PublicKey> keys = Map.of();
        assertThrows(Dsse.BadEnvelopeException.class, () -> Dsse.open("not json", keys));
        assertThrows(Dsse.BadEnvelopeException.class, () -> Dsse.open("{\"payloadType\":\"application/vnd.in-toto+json\",\"payload\":\"%%\",\"signatures\":[]}", keys));
        assertThrows(Dsse.BadEnvelopeException.class, () -> Dsse.open("{\"payloadType\":\"application/vnd.in-toto+json\",\"payload\":\"e30=\",\"signatures\":[]}", keys));
    }

    @Test
    void jsonRoundTripsAndKeepsKeyOrder() throws Exception {
        Map<String, Object> value = Json.object("b", 1L, "a", List.of("x", true, Json.object()), "c", "line\n\"quoted\"");
        String text = Json.write(value);
        assertEquals("{\"b\":1,\"a\":[\"x\",true,{}],\"c\":\"line\\n\\\"quoted\\\"\"}", text);
        assertEquals(value, Json.read(text));
    }

    @Test
    void jsonRefusesDuplicatesAndTrailingText() {
        assertThrows(Json.BadJsonException.class, () -> Json.read("{\"a\":1,\"a\":2}"));
        assertThrows(Json.BadJsonException.class, () -> Json.read("{} x"));
        assertThrows(Json.BadJsonException.class, () -> Json.read("[1,"));
    }
}
