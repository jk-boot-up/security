package com.jk.explore.security.racepg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Which errors are retried: only the ones the database raises because of timing. Needs no database. */
class SqlStatesTest {

    @Test
    void serializationFailuresAndDeadlocksAreRetried() {
        assertTrue(SqlStates.retry("40001"));
        assertTrue(SqlStates.retry("40P01"));
    }

    @Test
    void aBrokenRuleOrALockTimeoutIsNotRetriedAtOnce() {
        assertFalse(SqlStates.retry("23514"));
        assertFalse(SqlStates.retry("55P03"));
        assertFalse(SqlStates.retry("42P01"));
    }

    @Test
    void codesHaveTheirPostgresNames() {
        assertEquals("23514 check_violation", SqlStates.named("23514"));
        assertEquals("40P01 deadlock_detected", SqlStates.named("40P01"));
        assertEquals("unknown", SqlStates.of("99999").name());
    }
}
