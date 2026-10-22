package com.jk.explore.security.apispring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** The Redis-backed budget, with a real Redis in Docker. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisLimiterTest {

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
    void twoServersShareOneBudget() throws Exception {
        try (Running a = Running.start("safe", redis); Running b = Running.start("safe", redis)) {
            int answered = 0;
            for (int i = 0; i < 400; i++) {
                if ((i % 2 == 0 ? a : b).client().get("/accounts/acc-0001", "ana").status() == 200) {
                    answered++;
                }
            }
            assertEquals(50, answered);
        }
    }

    @Test
    void theBudgetIsExactUnderConcurrentRequests() throws Exception {
        try (Running api = Running.start("safe", redis); ExecutorService pool = Executors.newFixedThreadPool(16)) {
            List<Callable<Integer>> calls = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                calls.add(() -> api.client().get("/accounts/acc-0001", "ana").status());
            }
            int answered = 0;
            for (Future<Integer> f : pool.invokeAll(calls)) {
                if (f.get() == 200) {
                    answered++;
                }
            }
            assertEquals(50, answered, "the script takes a token and writes it back in one step");
        }
    }

    @Test
    void theBucketRefillsFromTheClockAndExpires() throws Exception {
        try (Running api = Running.start("safe", redis)) {
            Client c = api.client();
            for (int i = 0; i < 60; i++) {
                c.get("/accounts/acc-0001", "ana");
            }
            assertEquals(429, c.get("/accounts/acc-0001", "ana").status());
            assertTrue(Long.parseLong(redis.cli(true, "pttl", "rate:ana")) > 0, "an idle caller's bucket must not stay in Redis for ever");
            api.bean(ManualClock.class).advance(Duration.ofSeconds(1));
            assertEquals(200, c.get("/accounts/acc-0001", "ana").status());
            assertEquals(429, c.get("/accounts/acc-0001", "ana").status());
        }
    }

    @Test
    void theApiRefusesWhenRedisStopsAnsweringAndRecovers() throws Exception {
        try (Running api = Running.start("safe", redis)) {
            Client c = api.client();
            assertEquals(200, c.get("/accounts/acc-0001", "ana").status());
            redis.pause();
            try {
                assertEquals(503, c.get("/accounts/acc-0001", "ana").status());
            } finally {
                redis.unpause();
            }
            assertNotEquals(503, c.get("/accounts/acc-0001", "ana").status());
        }
    }
}
