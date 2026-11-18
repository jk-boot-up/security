package com.jk.explore.security.siembasics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** A long log line is shown cut into pieces; nothing is lost and no piece is too long. */
class WrapTest {

    @Test
    void theWrappedLineJoinsBackToTheOriginal() {
        String line = "{\"ts\":\"2026-10-23T09:20:00Z\",\"type\":\"login\",\"user\":\"alice\",\"ip\":\"203.0.113.5\",\"ok\":true}";
        List<String> pieces = SiemBasicsDemo.wrap(line, 56);
        assertEquals(line, String.join("", pieces));
        assertTrue(pieces.stream().allMatch(p -> p.length() <= 56));
    }

    @Test
    void aSyslogLineIsCutAtSpaces() {
        String line = "Oct 23 09:12:10 bastion-01 sshd[6040]: Failed password for alice from 203.0.113.5 port 55123 ssh2";
        assertEquals(line, String.join(" ", SiemBasicsDemo.wrap(line, 56)));
    }
}
