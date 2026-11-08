package com.jk.explore.security.hashing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** The rounds done by hand must give exactly the hash PBKDF2 itself stores. */
class RoundStepsTest {

    @Test
    void roundsByHandGiveThePbkdf2Hash() {
        byte[] salt = new CountingSalts().next();
        String stored = new Pbkdf2Hasher(new WorkMeter(), new CountingSalts(), 1000).hash("welcome1");
        RoundSteps.Steps steps = RoundSteps.of("welcome1", salt, 1000);
        assertEquals(stored.split("\\$")[3], HexFormat.of().formatHex(steps.hash()));
        assertEquals(3, steps.firstRounds().size());
    }

    @Test
    void oneRoundIsJustTheFirstHmac() {
        RoundSteps.Steps steps = RoundSteps.of("pw", new byte[16], 1);
        assertEquals(HexFormat.of().formatHex(steps.lastRound()), HexFormat.of().formatHex(steps.hash()));
    }
}
