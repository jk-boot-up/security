package com.jk.explore.security.races;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The same three withdrawals as real Java code, for the tests that run them on real threads. The demo uses the step-by-step model
 * instead, because real thread timing differs from run to run.
 */
final class RealAccounts {

    private RealAccounts() {
    }

    /** Check, then act, with nothing to stop another thread in between. The hook runs between the check and the write. */
    static final class Unsafe {
        private volatile long balance;

        Unsafe(long balance) {
            this.balance = balance;
        }

        boolean withdraw(long amount, Runnable betweenCheckAndWrite) {
            long seen = balance;
            if (seen < amount) {
                return false;
            }
            betweenCheckAndWrite.run();
            balance = seen - amount;
            return true;
        }

        long balance() {
            return balance;
        }
    }

    /** The check and the write inside one lock. */
    static final class Locked {
        private final ReentrantLock lock = new ReentrantLock();
        private long balance;

        Locked(long balance) {
            this.balance = balance;
        }

        boolean withdraw(long amount) {
            lock.lock();
            try {
                if (balance < amount) {
                    return false;
                }
                balance -= amount;
                return true;
            } finally {
                lock.unlock();
            }
        }

        long balance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }
    }

    /** No lock: a compare-and-set that retries when another thread wrote first. */
    static final class CompareAndSet {
        private final AtomicLong balance;

        CompareAndSet(long balance) {
            this.balance = new AtomicLong(balance);
        }

        boolean withdraw(long amount) {
            while (true) {
                long seen = balance.get();
                if (seen < amount) {
                    return false;
                }
                if (balance.compareAndSet(seen, seen - amount)) {
                    return true;
                }
            }
        }

        long balance() {
            return balance.get();
        }
    }
}
