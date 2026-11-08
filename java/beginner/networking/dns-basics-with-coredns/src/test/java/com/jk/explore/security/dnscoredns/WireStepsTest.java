package com.jk.explore.security.dnscoredns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The bytes of act one, worked out without Docker, match the golden output and decode back to the same answer. */
class WireStepsTest {

    private static final String TRUST = "  the client trusts it because the id and the question match, and nothing else\n";

    @Test
    void goldenOutputHoldsTheBytesRightAfterActOne() throws Exception {
        String golden = Files.readString(Path.of("docs", "expected-output.txt"));
        assertTrue(golden.contains(TRUST + WireSteps.text()), "regenerate docs/expected-output.txt");
    }

    @Test
    void timeToLiveAndAddressAreReadFromTheBytes() throws Exception {
        byte[] answer = DnsMessage.query(WireSteps.ID, CoreDnsDemo.WWW)
                .answeredWith(ShopZone.GOOD_TTL, Address.parse(ShopZone.GOOD_ADDRESS)).encode();
        assertEquals(50, answer.length);
        assertEquals("00 00 01 2c", WireSteps.hex(answer, 40, 4));
        assertEquals(300, WireSteps.number(answer, 40, 4));
        assertEquals("cb 00 71 0a", WireSteps.hex(answer, 46, 4));
        assertEquals("203.0.113.10", Address.text((int) WireSteps.number(answer, 46, 4)));
        DnsMessage.Answer decoded = DnsMessage.decode(answer).answers().get(0);
        assertEquals(300, decoded.ttl());
        assertEquals(CoreDnsDemo.WWW, decoded.name());
    }

    @Test
    void theQuestionIsWhatTheClientSends() throws Exception {
        byte[] question = DnsMessage.query(WireSteps.ID, CoreDnsDemo.WWW).encode();
        assertEquals(34, question.length);
        assertEquals("03 www 04 shop 07 example 00", WireSteps.labels(question, 12));
        assertEquals("10 01", WireSteps.hex(question, 0, 2));
    }
}
