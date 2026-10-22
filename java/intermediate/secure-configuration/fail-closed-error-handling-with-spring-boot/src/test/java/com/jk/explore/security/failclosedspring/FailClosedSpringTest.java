package com.jk.explore.security.failclosedspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Each test starts a real bank and a real fraud-check server on free loopback ports. */
class FailClosedSpringTest {

    private final Wire wire = new Wire();

    @Test
    void openVersionApprovesWhenTheCheckIsBroken() throws Exception {
        try (Running bank = Running.start("default", "bank.mode=open")) {
            assertEquals(403, wire.pay(bank.url(), FailClosedSpringDemo.RISKY).status());
            bank.fraud().mode(FraudServer.Mode.DROP);
            assertEquals(200, wire.pay(bank.url(), FailClosedSpringDemo.RISKY).status());
        }
    }

    @ParameterizedTest
    @EnumSource(value = FraudServer.Mode.class, names = {"ERROR", "GARBAGE", "SLOW", "DROP"})
    void closedVersionRefusesEveryKindOfBrokenCheck(FraudServer.Mode mode) throws Exception {
        try (Running bank = Running.start("default", "bank.breaker.threshold=1000")) {
            bank.fraud().mode(mode);
            Wire.Reply r = wire.pay(bank.url(), FailClosedSpringDemo.COFFEE);
            assertEquals(503, r.status());
            assertTrue(r.body().contains("quoting reference REF-0001"));
            assertFalse(r.body().contains("approved\":true"));
        }
    }

    @Test
    void closedVersionApprovesOnlyWhenTheCheckRanAndPassed() throws Exception {
        try (Running bank = Running.start("default")) {
            assertEquals(200, wire.pay(bank.url(), FailClosedSpringDemo.COFFEE).status());
            Wire.Reply risky = wire.pay(bank.url(), FailClosedSpringDemo.RISKY);
            assertEquals(403, risky.status());
            assertTrue(risky.body().contains("looks fraudulent"));
        }
    }

    @Test
    void aSlowCheckIsCutOffAtTheBudget() throws Exception {
        try (Running bank = Running.start("default", "bank.breaker.threshold=1000")) {
            bank.fraud().mode(FraudServer.Mode.SLOW);
            long start = System.nanoTime();
            assertEquals(503, wire.pay(bank.url(), FailClosedSpringDemo.COFFEE).status());
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 1900);
            assertEquals(Map.of("fraud check too slow", 1), bank.bean(RefusalWatcher.class).byReason());
        }
    }

    @Test
    void breakerOpensAfterThreeFailuresAndClosesAfterTheCoolDown() throws Exception {
        try (Running bank = Running.start("default")) {
            bank.fraud().mode(FraudServer.Mode.ERROR);
            for (int i = 0; i < 3; i++) {
                assertEquals(503, wire.pay(bank.url(), FailClosedSpringDemo.COFFEE).status());
            }
            assertEquals(Breaker.State.OPEN, bank.bean(Breaker.class).state());
            int before = bank.fraud().calls();
            assertEquals(503, wire.pay(bank.url(), FailClosedSpringDemo.COFFEE).status());
            assertEquals(before, bank.fraud().calls(), "an open breaker must not call the fraud service");
            bank.fraud().mode(FraudServer.Mode.UP);
            bank.bean(MutableClock.class).advance(Duration.ofSeconds(31));
            assertEquals(Breaker.State.HALF_OPEN, bank.bean(Breaker.class).state());
            assertEquals(200, wire.pay(bank.url(), FailClosedSpringDemo.COFFEE).status());
            assertEquals(Breaker.State.CLOSED, bank.bean(Breaker.class).state());
        }
    }

    @Test
    void aFailedTrialInHalfOpenOpensTheBreakerAgain() throws Exception {
        try (Running bank = Running.start("default")) {
            bank.fraud().mode(FraudServer.Mode.ERROR);
            for (int i = 0; i < 3; i++) {
                wire.pay(bank.url(), FailClosedSpringDemo.COFFEE);
            }
            bank.bean(MutableClock.class).advance(Duration.ofSeconds(31));
            assertEquals(503, wire.pay(bank.url(), FailClosedSpringDemo.COFFEE).status());
            assertEquals(Breaker.State.OPEN, bank.bean(Breaker.class).state());
        }
    }

    @Test
    void retryDoublesTheCallsToAStrugglingService() throws Exception {
        try (Running bank = Running.start("default", "bank.breaker.threshold=1000")) {
            bank.fraud().mode(FraudServer.Mode.ERROR);
            wire.pay(bank.url(), FailClosedSpringDemo.COFFEE);
            assertEquals(2, bank.fraud().calls());
        }
    }

    @Test
    void refusalsRaiseAnAlert() throws Exception {
        try (Running bank = Running.start("default", "bank.breaker.threshold=1000")) {
            bank.fraud().mode(FraudServer.Mode.ERROR);
            for (int i = 0; i < 4; i++) {
                wire.pay(bank.url(), FailClosedSpringDemo.COFFEE);
            }
            assertFalse(bank.bean(RefusalWatcher.class).alert());
            wire.pay(bank.url(), FailClosedSpringDemo.COFFEE);
            assertTrue(bank.bean(RefusalWatcher.class).alert());
        }
    }

    @Test
    void onlyAUnitOfWorkUndoesTheDebit() throws Exception {
        try (Running bank = Running.start("default")) {
            Ledger ledger = bank.bean(Ledger.class);
            Transfers t = bank.bean(Transfers.class);
            ledger.reset();
            assertThrows(FrozenAccountException.class, () -> t.withoutTransaction("ana", "ben", 25000));
            assertEquals(75000, ledger.cents("ana"));
            ledger.reset();
            assertThrows(FrozenAccountException.class, () -> t.transactionalButChecked("ana", "ben", 25000));
            assertEquals(75000, ledger.cents("ana"));
            ledger.reset();
            assertThrows(FrozenAccountException.class, () -> t.callsItsOwnTransactionalMethod("ana", "ben", 25000));
            assertEquals(75000, ledger.cents("ana"));
            ledger.reset();
            assertThrows(FrozenAccountException.class, () -> t.unitOfWork("ana", "ben", 25000));
            assertEquals(100000, ledger.cents("ana"));
            assertEquals(100000, ledger.total());
        }
    }

    @Test
    void debuggingSettingsLeakAndTheErrorHandlerDoesNot() throws Exception {
        try (Running bank = Running.start("leaky")) {
            bank.fraud().mode(FraudServer.Mode.DROP);
            Wire.Reply r = wire.pay(bank.url(), FailClosedSpringDemo.RISKY);
            assertTrue(r.body().contains(FailClosedSpringDemo.CARD));
            assertTrue(r.body().contains("127.0.0.1"));
        }
        try (Running bank = Running.start("default")) {
            bank.fraud().mode(FraudServer.Mode.DROP);
            Wire.Reply r = wire.pay(bank.url(), FailClosedSpringDemo.RISKY);
            assertFalse(r.body().contains(FailClosedSpringDemo.CARD));
            assertFalse(r.body().contains("127.0.0.1"));
            assertFalse(r.body().contains(".java"));
            String logged = bank.bean(SecurityLog.class).lines().get(0);
            assertTrue(logged.contains(r.reference()) && logged.contains("1111") && !logged.contains(FailClosedSpringDemo.CARD));
        }
    }

    @Test
    void anUnexpectedExceptionAlsoGetsTheGenericAnswer() throws Exception {
        try (Running bank = Running.start("default")) {
            Wire.Reply r = wire.pay(bank.url(), "this is not json");
            assertEquals(500, r.status());
            assertFalse(r.body().contains("Exception"));
            assertFalse(r.body().contains(".java"));
        }
    }
}
