package com.jk.explore.security.dnscoredns;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class DnsMessageTest {

    @Test
    void queryHasTheRfc1035Layout() throws Exception {
        byte[] bytes = DnsMessage.query(0xabcd, "example.com").encode();
        assertEquals("abcd01000001000000000000076578616d706c6503636f6d0000010001", HexFormat.of().formatHex(bytes));
    }

    @Test
    void answerNameIsAPointerToTheQuestion() throws Exception {
        byte[] bytes = DnsMessage.query(1, "example.com").answeredWith(300, Address.parse("93.184.216.34")).encode();
        int answerStart = 12 + 13 + 4;
        assertEquals(0xc0, bytes[answerStart] & 0xff);
        assertEquals(0x0c, bytes[answerStart + 1] & 0xff);
        assertEquals(answerStart + 2 + 10 + 4, bytes.length);
    }

    @Test
    void sharedSuffixesAreCompressedAndDecodeBack() throws Exception {
        DnsMessage message = new DnsMessage(7, true, 0,
                java.util.List.of(new DnsMessage.Question("www.shop.example.com", 1)),
                java.util.List.of(new DnsMessage.Answer("shop.example.com", 60, 1), new DnsMessage.Answer("www.shop.example.com", 60, 2)));
        DnsMessage again = DnsMessage.decode(message.encode());
        assertEquals("shop.example.com", again.answers().get(0).name());
        assertEquals("www.shop.example.com", again.answers().get(1).name());
        assertEquals(2, again.answers().get(1).address());
    }

    @Test
    void roundTripKeepsIdTtlAndAddress() throws Exception {
        DnsMessage again = DnsMessage.decode(DnsMessage.query(0xbeef, "a.b").answeredWith(0x12345678L, Address.parse("10.1.2.3")).encode());
        assertEquals(0xbeef, again.id());
        assertTrue(again.response());
        assertEquals(0x12345678L, again.answers().get(0).ttl());
        assertEquals("10.1.2.3", Address.text(again.answers().get(0).address()));
    }

    @Test
    void queryIsNotMarkedAsResponse() throws Exception {
        assertFalse(DnsMessage.decode(DnsMessage.query(1, "a.b").encode()).response());
    }

    @Test
    void pointerLoopIsRefused() {
        byte[] loop = new byte[17];
        loop[5] = 1;
        loop[12] = (byte) 0xc0;
        loop[13] = 12;
        assertThrows(BadMessageException.class, () -> DnsMessage.decode(loop));
    }

    @Test
    void pointerOutsideTheMessageIsRefused() {
        byte[] bytes = new byte[20];
        bytes[5] = 1;
        bytes[12] = (byte) 0xc3;
        bytes[13] = (byte) 0xff;
        assertThrows(BadMessageException.class, () -> DnsMessage.decode(bytes));
    }

    @Test
    void shortAndCutMessagesAreRefused() throws Exception {
        assertThrows(BadMessageException.class, () -> DnsMessage.decode(new byte[5]));
        byte[] whole = DnsMessage.query(1, "example.com").answeredWith(1, 1).encode();
        for (int cut = 13; cut < whole.length; cut += 3) {
            byte[] part = java.util.Arrays.copyOf(whole, cut);
            assertThrows(BadMessageException.class, () -> DnsMessage.decode(part), "cut at " + cut);
        }
    }

    @Test
    void badNamesCannotBeEncoded() {
        assertThrows(BadMessageException.class, () -> DnsMessage.query(1, "a".repeat(64) + ".com").encode());
        assertThrows(BadMessageException.class, () -> DnsMessage.query(1, "a..com").encode());
        assertThrows(BadMessageException.class, () -> DnsMessage.query(1, "a.".repeat(130) + "com").encode());
    }

    @Test
    void identicalMessagesEncodeIdentically() throws Exception {
        assertArrayEquals(DnsMessage.query(9, "x.y").encode(), DnsMessage.query(9, "x.y").encode());
    }
}
