package com.jk.explore.security.privacyeng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PurposesPseudonymsSeedTest {

    @Test
    void sevenOfTheNinePersonalColumnsHaveAPurpose() {
        assertEquals(9, Purposes.PERSONAL_COLUMNS.size());
        assertEquals(7, Purposes.needed().size());
        assertEquals(List.of("phone", "gender"), Purposes.withoutPurpose());
    }

    @Test
    void everyColumnAPurposeNeedsIsAPersonalColumn() {
        for (var needs : Purposes.NEEDS.values()) {
            assertTrue(Purposes.PERSONAL_COLUMNS.containsAll(needs));
        }
    }

    @Test
    void aPseudonymIsStableUnderOneKeyAndDiffersUnderAnotherAndPerCustomer() {
        Pseudonyms a = new Pseudonyms("key-one");
        assertEquals(a.of(5), new Pseudonyms("key-one").of(5));
        assertNotEquals(a.of(5), new Pseudonyms("key-two").of(5));
        assertNotEquals(a.of(5), a.of(6));
        assertEquals(16, a.of(5).length());
    }

    @Test
    void aPseudonymDoesNotContainTheId() {
        assertTrue(!new Pseudonyms("k").of(1234).contains("1234"));
    }

    @Test
    void namesAndEmailsAreMadeFromTheIdAndAreDistinct() {
        assertEquals("customer17@mail.example", Seed.emailOf(17));
        assertNotEquals(Seed.nameOf(17), Seed.nameOf(18));
        assertTrue(Seed.nameOf(17).endsWith(" 17"));
    }

    @Test
    void theRetentionLimitsAreSevenTwoAndOneYears() {
        assertEquals(7, Retention.TRANSACTION_YEARS);
        assertEquals(2, Retention.CONSENT_YEARS);
        assertEquals(1, Retention.NOTE_YEARS);
    }
}
