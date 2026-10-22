package com.jk.explore.security.sessionredis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** Real bank servers and one real Redis in Docker. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionRedisTest {

    private RedisLab redis;

    @BeforeAll
    void startRedis() {
        redis = RedisLab.start();
    }

    @AfterAll
    void stopRedis() {
        redis.close();
    }

    @BeforeEach
    void clean() {
        redis.cli(true, "flushall");
    }

    @Test
    void redisRefusesAClientWithNoPassword() {
        assertTrue(redis.cli(false, "ping").startsWith("NOAUTH"));
    }

    @Test
    void signInWorksAndTheCookieHasEveryProtection() throws Exception {
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            Browser.Reply in = ana.signIn("ana");
            assertEquals(200, in.status());
            assertEquals(List.of(), SessionRedisDemo.cookieProblems(in.setCookies().get(0)));
            assertEquals(200, ana.get("/account").status());
        }
    }

    @Test
    void aWrongPasswordGivesNoSession() throws Exception {
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            assertEquals(401, ana.signIn("ana", "wrong").status());
            assertEquals(401, ana.get("/account").status());
            assertEquals(0, SessionRedisDemo.records(redis).size());
        }
    }

    @Test
    void theCookieIsTheRedisKeyEncodedSoStolenKeysAreLiveSessions() throws Exception {
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            String key = SessionRedisDemo.records(redis).iterator().next();
            String id = key.substring("spring:session:sessions:".length());
            assertEquals(Base64.getEncoder().encodeToString(id.getBytes()), ana.cookie());
            Browser thief = bank.browser();
            thief.setCookie(Base64.getEncoder().encodeToString(id.getBytes()));
            assertEquals(200, thief.get("/account").status());
        }
    }

    @Test
    void sessionFixationWorksWhenTheIdIsKeptAndFailsWithTheDefault() throws Exception {
        for (String mode : List.of("none", "change")) {
            try (Running bank = Running.start(redis, "bank.fixation=" + mode)) {
                Browser ana = bank.browser();
                ana.get("/welcome");
                Browser stranger = bank.browser();
                stranger.setCookie(ana.cookie());
                String planted = ana.cookie();
                ana.signIn("ana");
                assertEquals(mode.equals("none"), planted.equals(ana.cookie()));
                assertEquals(mode.equals("none") ? 200 : 401, stranger.get("/account").status());
                assertEquals(200, ana.get("/account").status());
            }
        }
    }

    @Test
    void anIdleSessionIsRefusedAndRedisKeepsTheRecordFiveMoreMinutes() throws Exception {
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            String key = SessionRedisDemo.records(redis).iterator().next();
            long ttl = Long.parseLong(redis.cli(true, "ttl", key));
            assertTrue(ttl > 2 && ttl <= 302, "ttl was " + ttl);
            Thread.sleep(2500);
            assertEquals(401, ana.get("/account").status());
            assertEquals("1", redis.cli(true, "exists", key));
        }
    }

    @Test
    void aBusySessionStillDiesAtTheAbsoluteLimit() throws Exception {
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            bank.bean(OffsetClock.class).advance(Duration.ofHours(7).plusMinutes(59));
            assertEquals(200, ana.get("/account").status());
            bank.bean(OffsetClock.class).advance(Duration.ofMinutes(2));
            assertEquals(401, ana.get("/account").status());
        }
    }

    @Test
    void signOutEndsTheSessionOnTheServer() throws Exception {
        try (Running bank = Running.start(redis, "spring.session.timeout=5m")) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            Browser copy = bank.browser();
            copy.setCookie(ana.cookie());
            assertEquals(200, ana.post("/logout", "").status());
            assertEquals(401, copy.get("/account").status());
            assertEquals(0, SessionRedisDemo.records(redis).size());
        }
    }

    @Test
    void signOutEverywhereEndsEveryDevice() throws Exception {
        try (Running bank = Running.start(redis, "spring.session.timeout=5m")) {
            List<Browser> devices = List.of(bank.browser(), bank.browser(), bank.browser());
            devices.forEach(d -> signIn(d, "ana"));
            Browser ben = bank.browser();
            ben.signIn("ben");
            assertEquals("ended 3 sessions", devices.get(1).post("/sign-out-everywhere", "").body());
            devices.forEach(d -> assertEquals(401, get(d, "/account")));
            assertEquals(200, ben.get("/account").status(), "another customer's session is not touched");
        }
    }

    @Test
    void changingThePasswordEndsTheOtherSessionsAndRenewsThisOne() throws Exception {
        try (Running bank = Running.start(redis, "spring.session.timeout=5m")) {
            Browser phone = bank.browser();
            Browser laptop = bank.browser();
            phone.signIn("ana");
            laptop.signIn("ana");
            String before = laptop.cookie();
            assertEquals("ended 1 other sessions", laptop.post("/password", "").body());
            assertNotEquals(before, laptop.cookie());
            assertEquals(200, laptop.get("/account").status());
            assertEquals(401, phone.get("/account").status());
            Browser stale = bank.browser();
            stale.setCookie(before);
            assertEquals(401, stale.get("/account").status());
        }
    }

    @Test
    void aFourthSignInEndsTheOldestSession() throws Exception {
        try (Running bank = Running.start(redis, "spring.session.timeout=5m", "bank.max-sessions=3")) {
            List<Browser> devices = List.of(bank.browser(), bank.browser(), bank.browser(), bank.browser());
            for (Browser d : devices) {
                d.signIn("ana");
                Thread.sleep(50);
            }
            assertEquals(401, devices.get(0).get("/account").status());
            for (Browser d : devices.subList(1, 4)) {
                assertEquals(200, d.get("/account").status());
            }
        }
    }

    @Test
    void twoServersShareSessionsOnlyThroughTheSameStore() throws Exception {
        try (Running a = Running.start(redis, "spring.session.timeout=5m"); Running b = Running.start(redis, "spring.session.timeout=5m")) {
            Browser ana = a.browser();
            ana.signIn("ana");
            assertEquals(200, ana.at(b.url()).get("/account").status());
        }
        try (Running a = Running.start(redis, "spring.session.timeout=5m", "spring.session.data.redis.namespace=spring:session:a");
                Running b = Running.start(redis, "spring.session.timeout=5m", "spring.session.data.redis.namespace=spring:session:b")) {
            Browser ana = a.browser();
            ana.signIn("ana");
            assertEquals(401, ana.at(b.url()).get("/account").status());
        }
    }

    @Test
    void theBankRefusesWhenRedisStopsAnsweringAndRecoversAfterwards() throws Exception {
        try (Running bank = Running.start(redis, "spring.session.timeout=5m")) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            redis.pause();
            try {
                assertNotEquals(200, ana.get("/account").status());
                assertNotEquals(200, bank.browser().signIn("ana").status());
            } finally {
                redis.unpause();
            }
            assertEquals(200, ana.get("/account").status());
        }
    }

    @Test
    void strangersFillRedisOnlyWhileTheRequestCacheIsOn() throws Exception {
        try (Running bank = Running.start(redis, "bank.save-requests=true")) {
            for (int i = 0; i < 5; i++) {
                bank.browser().get("/account");
            }
            assertEquals(5, SessionRedisDemo.records(redis).size());
        }
        redis.cli(true, "flushall");
        try (Running bank = Running.start(redis)) {
            for (int i = 0; i < 5; i++) {
                bank.browser().get("/account");
            }
            assertFalse(SessionRedisDemo.records(redis).size() > 0);
        }
    }

    private static void signIn(Browser device, String user) {
        try {
            device.signIn(user);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static int get(Browser device, String path) {
        try {
            return device.get(path).status();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
