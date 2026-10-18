package com.jk.explore.security.races;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.LongPredicate;
import org.junit.jupiter.api.Test;

/** The real Java versions on real threads. The bad order is forced with a barrier, never left to chance. */
class RealThreadsTest {

    @Test
    void checkThenActOverdrawsWhenBothThreadsPassTheCheckFirst() throws Exception {
        RealAccounts.Unsafe account = new RealAccounts.Unsafe(100);
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        Runnable waitForTheOther = () -> {
            try {
                bothChecked.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = pool.submit(() -> account.withdraw(80, waitForTheOther));
            Future<Boolean> b = pool.submit(() -> account.withdraw(80, waitForTheOther));
            assertTrue(a.get(10, TimeUnit.SECONDS));
            assertTrue(b.get(10, TimeUnit.SECONDS));
        }
        assertEquals(20, account.balance());
    }

    private static int successes(int threads, LongPredicate withdraw) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            CyclicBarrier start = new CyclicBarrier(threads);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return withdraw.test(10);
                }));
            }
            int ok = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    ok++;
                }
            }
            return ok;
        }
    }

    @Test
    void aLockedAccountNeverOverdrawsUnderLoad() throws Exception {
        for (int round = 0; round < 50; round++) {
            RealAccounts.Locked account = new RealAccounts.Locked(100);
            assertEquals(10, successes(24, account::withdraw));
            assertEquals(0, account.balance());
        }
    }

    @Test
    void aCompareAndSetAccountNeverOverdrawsUnderLoad() throws Exception {
        for (int round = 0; round < 50; round++) {
            RealAccounts.CompareAndSet account = new RealAccounts.CompareAndSet(100);
            assertEquals(10, successes(24, account::withdraw));
            assertEquals(0, account.balance());
        }
    }

    @Test
    void oneIdempotencyKeySentByManyThreadsMovesTheMoneyOnce() throws Exception {
        for (int round = 0; round < 50; round++) {
            Bank bank = new Bank(true).open("ANA-1", 1000).open("BEN-2", 0);
            Bank.Transfer rent = new Bank.Transfer("ANA-1", "BEN-2", 300);
            int threads = 16;
            try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
                CyclicBarrier start = new CyclicBarrier(threads);
                List<Future<String>> answers = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    answers.add(pool.submit(() -> {
                        start.await(10, TimeUnit.SECONDS);
                        return bank.transfer("rent-october", rent);
                    }));
                }
                for (Future<String> answer : answers) {
                    assertTrue(answer.get(10, TimeUnit.SECONDS).endsWith("moved 300"));
                }
            }
            assertEquals(700, bank.balance("ANA-1"));
            assertEquals(300, bank.balance("BEN-2"));
        }
    }
}
