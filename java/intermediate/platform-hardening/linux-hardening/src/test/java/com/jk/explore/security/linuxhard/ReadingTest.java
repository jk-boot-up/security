package com.jk.explore.security.linuxhard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The small readers, without any file system: mode digits, the kernel's hex addresses, and the build gate. */
class ReadingTest {

    @Test
    void aModeDigitReadsAsLetters() {
        assertEquals("rwx", Modes.rwx(7));
        assertEquals("r-x", Modes.rwx(5));
        assertEquals("rw-", Modes.rwx(6));
        assertEquals("r--", Modes.rwx(4));
        assertEquals("---", Modes.rwx(0));
    }

    @Test
    void theSpecialDigitNamesItsBits() {
        assertEquals("set-user-id", Modes.special(4));
        assertEquals("none", Modes.special(0));
        assertEquals("set-user-id + set-group-id + sticky", Modes.special(7));
    }

    @Test
    void theKernelWritesAddressesLittleEndian() {
        assertEquals("127.0.0.1", PortTable.address("0100007F"));
        assertEquals("0.0.0.0", PortTable.address("00000000"));
        assertEquals("192.168.1.10", PortTable.address("0A01A8C0"));
    }

    @Test
    void theStepsOfAFixAreWorkedOut() {
        assertEquals("0777 - 0002 = 0775", new Fix("/x", 0777, "-", 02, 0775, "").steps());
        assertEquals("0644 & 0600 = 0600", new Fix("/x", 0644, "&", 0600, 0600, "").steps());
    }

    @Test
    void theBuildGateFailsAtItsSeverityAndAbove() {
        BuildGate gate = new BuildGate(Severity.HIGH);
        assertTrue(gate.passes(List.of(new Finding("ssh-x11", Severity.LOW, "s", "m", false))));
        assertFalse(gate.passes(List.of(new Finding("default-account", Severity.HIGH, "s", "m", false))));
        assertTrue(gate.passes(List.of()));
    }
}
