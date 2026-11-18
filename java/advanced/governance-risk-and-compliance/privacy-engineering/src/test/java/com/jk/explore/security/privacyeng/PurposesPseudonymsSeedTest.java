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
        for (Purposes.Purpose p : Purposes.ALL) {
            assertTrue(Purposes.PERSONAL_COLUMNS.containsAll(p.needs()));
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

    @Test
    void thePseudonymIsTheFirstEightBytesOfTheWholeMac() throws Exception {
        PseudonymSteps steps = PseudonymSteps.of("analytics-key-one", 17);
        assertEquals("customer:17", steps.input());
        assertEquals(64, steps.fullMac().length());
        assertTrue(steps.fullMac().startsWith(steps.pseudonym()));
        assertEquals(new Pseudonyms("analytics-key-one").of(17), steps.pseudonym());
    }

    @Test
    void coarseValuesKeepThreeDigitsAndTheDecade() {
        assertEquals("108", Coarse.postcode("10845"));
        assertEquals("1960s", Coarse.decade(java.time.LocalDate.of(1969, 11, 16)));
        assertEquals("2000s", Coarse.decade(java.time.LocalDate.of(2005, 1, 1)));
    }

    @Test
    void thePurposesAreInAFixedOrderWithShortCodes() {
        assertEquals(List.of("pay", "stmt", "mkt", "fraud", "id"), Purposes.ALL.stream().map(Purposes.Purpose::code).toList());
    }
}
