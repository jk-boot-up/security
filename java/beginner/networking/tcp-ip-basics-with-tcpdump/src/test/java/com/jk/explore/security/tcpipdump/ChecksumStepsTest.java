package com.jk.explore.security.tcpipdump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The by-hand checksum steps give the same checksum as the packet carries, and catch the damaged packet. */
class ChecksumStepsTest {

    @Test
    void theStepsGiveTheChecksumThePacketWasSentWith() throws Exception {
        ChecksumSteps steps = ChecksumSteps.forTcp(Traffic.lateSyn());
        assertEquals(6, steps.pseudoWords().size());
        assertEquals(10, steps.tcpWords().size());
        assertEquals(0x6440, steps.pseudoWords().get(0));
        assertEquals(0x38f2d, steps.total());
        assertEquals(0x8f30, steps.folded());
        assertEquals(0x70cf, steps.checksum());
        assertTrue(steps.matches());
    }

    @Test
    void theStepsAgreeWithTheChecksumClassForEveryCleanPacket() throws Exception {
        for (byte[] packet : Traffic.purchase()) {
            assertTrue(ChecksumSteps.forTcp(packet).matches());
        }
    }

    @Test
    void theDamagedPacketDoesNotMatch() throws Exception {
        ChecksumSteps steps = ChecksumSteps.forTcp(Traffic.damaged());
        assertEquals(0x70cf, steps.checksum());
        assertEquals(0x70cf ^ 0x5500, steps.stored());
        assertFalse(steps.matches());
    }

    @Test
    void wordsPadAnOddByteWithZero() {
        assertEquals(List.of(0x0102, 0x0300), ChecksumSteps.words(new byte[] {1, 2, 3}));
    }

    @Test
    void onlyTheCustomerFinishedAHandshake() throws Exception {
        Map<String, Boolean> handshakes = Capture.handshakes(PcapFile.write(Traffic.everything()));
        assertEquals(10, handshakes.size());
        assertTrue(handshakes.get("198.51.100.7"));
        assertEquals(1, handshakes.values().stream().filter(done -> done).count());
    }
}
