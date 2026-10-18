package com.jk.explore.security.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SessionManagerTest {

    private static final char[] PASSWORD = SessionDemo.PASSWORD.toCharArray();

    private final SimClock clock = new SimClock();

    private SessionManager bank(Settings settings) {
        return new SessionManager(clock, settings, new SessionStore(), SessionDemo.users());
    }

    private static String signIn(SessionManager bank, String current) {
        return bank.signIn(current, SessionDemo.ANA, PASSWORD).orElseThrow();
    }

    @Test
    void aVisitorSessionIsNotSignedIn() {
        SessionManager bank = bank(Settings.bank());
        assertEquals(Answer.NOT_SIGNED_IN, bank.account(bank.start()));
        assertEquals(Answer.UNKNOWN, bank.account(null));
        assertEquals(Answer.UNKNOWN, bank.account("not-an-id"));
    }

    @Test
    void aWrongPasswordOrUnknownNameStartsNothing() {
        SessionManager bank = bank(Settings.bank());
        assertTrue(bank.signIn(null, SessionDemo.ANA, "wrong".toCharArray()).isEmpty());
        assertTrue(bank.signIn(null, "mallory", PASSWORD).isEmpty());
        assertEquals(0, bank.store().size());
    }

    @Test
    void signInIssuesANewIdAndTheOldOneStopsWorking() {
        SessionManager bank = bank(Settings.bank());
        String before = bank.start();
        String after = signIn(bank, before);
        assertNotEquals(before, after);
        assertEquals(Answer.UNKNOWN, bank.account(before));
        assertEquals(Answer.ALLOWED, bank.account(after));
    }

    @Test
    void withoutRotationAPlantedIdIsSignedInToo() {
        SessionManager bank = bank(Settings.withoutRotation());
        String planted = bank.start();
        assertEquals(planted, signIn(bank, planted));
        assertEquals(Answer.ALLOWED, bank.account(planted));
    }

    @Test
    void stepUpIssuesANewIdAndKeepsTheSessionsAge() {
        SessionManager bank = bank(Settings.bank());
        String id = signIn(bank, null);
        clock.advance(SimClock.minutes(10));
        assertEquals(Answer.REAUTH_REQUIRED, bank.payment(id));
        String stepped = bank.stepUp(id, PASSWORD).orElseThrow();
        assertEquals(Answer.UNKNOWN, bank.account(id));
        assertEquals(Answer.ALLOWED, bank.payment(stepped));
        assertTrue(bank.stepUp(stepped, "wrong".toCharArray()).isEmpty());
        for (int i = 0; i < 46; i++) {
            clock.advance(SimClock.minutes(10));
            assertEquals(Answer.ALLOWED, bank.account(stepped));
        }
        clock.advance(SimClock.minutes(10));
        assertEquals(Answer.ABSOLUTE_EXPIRED, bank.account(stepped), "the step-up kept the session's original age");
    }

    @Test
    void idleTimeoutEndsTheSessionAtFifteenMinutes() {
        SessionManager bank = bank(Settings.bank());
        String id = signIn(bank, null);
        clock.advance(SimClock.minutes(15) - 1);
        assertEquals(Answer.ALLOWED, bank.account(id));
        clock.advance(SimClock.minutes(15));
        assertEquals(Answer.IDLE_EXPIRED, bank.account(id));
        assertEquals(Answer.UNKNOWN, bank.account(id));
    }

    @Test
    void absoluteTimeoutEndsABusySessionAtEightHours() {
        SessionManager bank = bank(Settings.bank());
        String id = signIn(bank, null);
        for (int i = 0; i < 47; i++) {
            clock.advance(SimClock.minutes(10));
            assertEquals(Answer.ALLOWED, bank.account(id));
        }
        clock.advance(SimClock.minutes(10));
        assertEquals(Answer.ABSOLUTE_EXPIRED, bank.account(id));
    }

    @Test
    void paymentsNeedARecentPassword() {
        SessionManager bank = bank(Settings.bank());
        String id = signIn(bank, null);
        clock.advance(SimClock.minutes(5));
        assertEquals(Answer.ALLOWED, bank.payment(id));
        clock.advance(1);
        assertEquals(Answer.REAUTH_REQUIRED, bank.payment(id));
        assertEquals(Answer.ALLOWED, bank.account(id));
    }

    @Test
    void signOutEndsTheSessionOnTheServer() {
        SessionManager bank = bank(Settings.bank());
        String id = signIn(bank, null);
        bank.signOut(id);
        assertEquals(Answer.UNKNOWN, bank.account(id));
    }

    @Test
    void signOutEverywhereEndsOnlyThatCustomersSessions() {
        SessionManager bank = bank(Settings.bank());
        signIn(bank, null);
        signIn(bank, null);
        String visitor = bank.start();
        assertEquals(2, bank.signOutEverywhere(SessionDemo.ANA));
        assertEquals(Answer.NOT_SIGNED_IN, bank.account(visitor));
    }

    @Test
    void aPasswordChangeEndsOtherSessionsAndRotatesThisOne() {
        SessionManager bank = bank(Settings.bank());
        String phone = signIn(bank, null);
        String laptop = signIn(bank, null);
        SessionManager.Changed changed = bank.changePassword(laptop, PASSWORD, "new-demo-password".toCharArray()).orElseThrow();
        assertEquals(1, changed.ended());
        assertEquals(Answer.UNKNOWN, bank.account(phone));
        assertEquals(Answer.UNKNOWN, bank.account(laptop));
        assertEquals(Answer.ALLOWED, bank.account(changed.id()));
        assertTrue(bank.signIn(null, SessionDemo.ANA, PASSWORD).isEmpty());
        assertTrue(bank.signIn(null, SessionDemo.ANA, "new-demo-password".toCharArray()).isPresent());
    }

    @Test
    void aPasswordChangeNeedsTheOldPassword() {
        SessionManager bank = bank(Settings.bank());
        String id = signIn(bank, null);
        assertTrue(bank.changePassword(id, "wrong".toCharArray(), "x".toCharArray()).isEmpty());
        assertEquals(Answer.ALLOWED, bank.account(id));
    }

    @Test
    void theLimitEndsTheOldestSession() {
        SessionManager bank = bank(Settings.bank());
        String first = signIn(bank, null);
        String second = signIn(bank, null);
        signIn(bank, null);
        String fourth = signIn(bank, null);
        assertEquals(Answer.UNKNOWN, bank.account(first));
        assertEquals(Answer.ALLOWED, bank.account(second));
        assertEquals(Answer.ALLOWED, bank.account(fourth));
    }

    @Test
    void serversNeedASharedStore() {
        Users users = SessionDemo.users();
        SessionManager a = new SessionManager(clock, Settings.bank(), new SessionStore(), users);
        SessionManager b = new SessionManager(clock, Settings.bank(), new SessionStore(), users);
        String id = a.signIn(null, SessionDemo.ANA, PASSWORD).orElseThrow();
        assertEquals(Answer.UNKNOWN, b.account(id));
        SessionStore shared = new SessionStore();
        SessionManager c = new SessionManager(clock, Settings.bank(), shared, users);
        SessionManager d = new SessionManager(clock, Settings.bank(), shared, users);
        assertEquals(Answer.ALLOWED, d.account(c.signIn(null, SessionDemo.ANA, PASSWORD).orElseThrow()));
    }
}
