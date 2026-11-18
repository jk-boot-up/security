package com.jk.explore.security.racepg;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * The online bank's races, run against a real PostgreSQL 18 in Docker, in five numbered acts. The requests are two connections
 * whose statements run in a written order, so the output is the same on every run; ports and container names are never printed.
 */
public class RaceConditionsDemo {

    static final String DRAW = "update accounts set balance = balance - 80, paid_out = paid_out + 80 where id = 'ANA-1'";
    static final String DRAW_IF_COVERED = DRAW + " and balance >= 80";
    static final String CLAIM_COUPON = "update coupons set used = true where code = 'SPRING20' and used = false";
    static final String CLAIM_KEY = "insert into payments (key, amount) values ('rent-october', 70) on conflict do nothing";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Db db = new Db()) {
            act1(out, db);
            act2(out, db);
            act3(out, db);
            act4(out, db);
            act5(out, db);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A "key   value" line inside a section. */
    private static void fact(StringBuilder out, String key, Object value) {
        line(out, "    " + String.format("%-13s", key) + value);
    }

    /** Runs on a thread of its own, so that the caller can watch it wait. */
    static <T> CompletableFuture<T> async(Callable<T> work) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return work.call();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    static int balance(Connection c, String id) throws SQLException {
        return Db.one(c, "select balance from accounts where id = ?", id);
    }

    static int paidOut(Db db) throws SQLException {
        try (Connection c = db.connect("read")) {
            return Db.one(c, "select sum(paid_out) from accounts");
        }
    }

    static int balanceOf(Db db, String id) throws SQLException {
        try (Connection c = db.connect("read")) {
            return balance(c, id);
        }
    }

    private static void act1(StringBuilder out, Db db) throws Exception {
        line(out, "ONE. Check, then act: two requests in the gap.");
        line(out, "  THE REQUESTS (ANA-1 holds 100, in PostgreSQL 18):");
        fact(out, "A and B", "withdraw 80 each, on two connections at once");
        fact(out, "each", "selects the balance, checks it in Java, updates");
        db.reset();
        line(out, "  THE STEPS (A reads, B reads, A writes, B writes):");
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int seenA = balance(a, "ANA-1");
            fact(out, "A reads", seenA + ", checks " + seenA + " >= 80: " + (seenA >= 80));
            int seenB = balance(b, "ANA-1");
            fact(out, "B reads", seenB + ", checks " + seenB + " >= 80: " + (seenB >= 80));
            Db.update(a, "update accounts set balance = ?, paid_out = paid_out + 80 where id = 'ANA-1'", seenA - 80);
            a.commit();
            fact(out, "A writes", "balance = " + (seenA - 80) + " (" + seenA + " - 80), commits");
            Db.update(b, "update accounts set balance = ?, paid_out = paid_out + 80 where id = 'ANA-1'", seenB - 80);
            b.commit();
            fact(out, "B writes", "balance = " + (seenB - 80) + " (" + seenB + " - 80), commits");
        }
        line(out, "  THE RESULT:");
        fact(out, "paid out", paidOut(db) + " from a balance of 100  <- BOTH CHECKS PASSED");
        fact(out, "balance", balanceOf(db, "ANA-1"));

        db.reset();
        line(out, "  AFTER THE FIX, ONE: THE RULE IN THE TABLE:");
        fact(out, "column", "balance int check (balance >= 0)");
        fact(out, "update", "set balance = balance - 80");
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            Db.update(a, DRAW);
            a.commit();
            fact(out, "A", "100 - 80 = " + balanceOf(db, "ANA-1") + ": paid");
            try {
                Db.update(b, DRAW);
                b.commit();
                fact(out, "B", "paid");
            } catch (SQLException e) {
                b.rollback();
                fact(out, "B", "20 - 80 = -60 < 0: " + SqlStates.named(e.getSQLState()));
            }
        }
        fact(out, "paid out", paidOut(db) + ", balance " + balanceOf(db, "ANA-1"));

        db.reset();
        line(out, "  AFTER THE FIX, TWO: THE CHECK INSIDE THE UPDATE:");
        fact(out, "update", "... where id = 'ANA-1' and balance >= 80");
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int rowsA = Db.update(a, DRAW_IF_COVERED);
            a.commit();
            int rowsB = Db.update(b, DRAW_IF_COVERED);
            b.commit();
            fact(out, "A", rowsA + " row changed: paid");
            fact(out, "B", rowsB + " rows changed (20 >= 80 is false): refused");
        }
        fact(out, "paid out", paidOut(db));

        db.reset();
        line(out, "  A SINGLE-USE COUPON SPRING20, 20 OFF (BEFORE THE FIX):");
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int usedA = Db.one(a, "select case when used then 1 else 0 end from coupons where code = 'SPRING20'");
            int usedB = Db.one(b, "select case when used then 1 else 0 end from coupons where code = 'SPRING20'");
            for (Connection c : List.of(a, b)) {
                Db.update(c, "update coupons set used = true, discount_given = discount_given + 20 where code = 'SPRING20'");
                c.commit();
            }
            fact(out, "A, B read", "used = " + (usedA == 1) + " and " + (usedB == 1));
            fact(out, "discount", "20 + 20 = " + Db.one(a, "select discount_given from coupons") + "  <- USED TWICE");
        }
        db.reset();
        line(out, "  AFTER THE FIX: CLAIMED IN ONE STATEMENT:");
        fact(out, "update", "set used = true where ... and used = false");
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int claimA = Db.update(a, CLAIM_COUPON);
            a.commit();
            int claimB = Db.update(b, CLAIM_COUPON);
            b.commit();
            fact(out, "A", claimA + " row: claimed, gets 20 off");
            fact(out, "B", claimB + " rows: already used, refused");
        }
    }

    private static void act2(StringBuilder out, Db db) throws Exception {
        line(out, "TWO. Row locks: making the check and the write one step.");
        db.reset();
        line(out, "  THE STEPS (select ... for update):");
        try (Connection a = db.connect("A")) {
            int seen = Db.one(a, "select balance from accounts where id = 'ANA-1' for update");
            fact(out, "A", "locks the row ANA-1 and reads " + seen);
            CompletableFuture<Integer> b = async(() -> {
                try (Connection c = db.connect("B")) {
                    int read = Db.one(c, "select balance from accounts where id = 'ANA-1' for update");
                    if (read >= 80) {
                        Db.update(c, DRAW);
                    }
                    c.commit();
                    return read;
                }
            });
            boolean waiting = db.waitUntilWaiting("B");
            fact(out, "B", "asks for the same row: waiting " + waiting);
            Db.update(a, DRAW);
            a.commit();
            fact(out, "A", "pays out 80 and commits: the lock is released");
            int read = b.get();
            fact(out, "B", "gets the row, reads " + read + ", " + read + " >= 80: " + (read >= 80) + ", refuses");
        }
        line(out, "  THE RESULT:");
        fact(out, "paid out", paidOut(db) + ", balance " + balanceOf(db, "ANA-1"));

        db.reset();
        line(out, "  WHEN B SHOULD NOT WAIT (A HOLDS THE ROW):");
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            Db.one(a, "select balance from accounts where id = 'ANA-1' for update");
            String nowait;
            try {
                Db.one(b, "select balance from accounts where id = 'ANA-1' for update nowait");
                nowait = "got the row";
            } catch (SQLException e) {
                b.rollback();
                nowait = "refused at once: " + SqlStates.named(e.getSQLState());
            }
            fact(out, "nowait", nowait);
            b.createStatement().execute("set lock_timeout = '300ms'");
            long start = System.nanoTime();
            String timeout;
            try {
                Db.one(b, "select balance from accounts where id = 'ANA-1' for update");
                timeout = "got the row";
            } catch (SQLException e) {
                b.rollback();
                timeout = "gave up: " + e.getSQLState();
            }
            fact(out, "timeout", "lock_timeout 300 ms, waited it: " + ((System.nanoTime() - start) / 1_000_000 >= 300) + ", " + timeout);
            int other = Db.one(b, "select count(*) from (select id from accounts order by id for update skip locked limit 1) t where id = 'BEN-2'");
            fact(out, "skip locked", "takes the next free row: BEN-2 " + (other == 1));
        }
    }

    /** Runs {@code work} until it succeeds or fails with an error that is not worth retrying; returns the number of attempts. */
    static int retrying(Callable<Boolean> work) throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                work.call();
                return attempt;
            } catch (SQLException e) {
                if (!SqlStates.retry(e.getSQLState())) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("gave up after 5 attempts");
    }

    private static void act3(StringBuilder out, Db db) throws Exception {
        line(out, "THREE. Isolation levels.");
        db.reset();
        line(out, "  READ COMMITTED (THE DEFAULT), EACH TAKES 30:");
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int seen = balance(a, "ANA-1");
            fact(out, "A reads", seen);
            Db.update(b, "update accounts set balance = balance - 30 where id = 'ANA-1'");
            b.commit();
            fact(out, "B", "takes 30 and commits: " + balanceOf(db, "ANA-1"));
            Db.update(a, "update accounts set balance = balance - 30 where id = 'ANA-1'");
            a.commit();
            fact(out, "A updates", "balance - 30 on the new value: 70 - 30 = " + balanceOf(db, "ANA-1"));
        }
        db.reset();
        line(out, "  REPEATABLE READ, THE SAME STEPS:");
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            a.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            int seen = balance(a, "ANA-1");
            fact(out, "A reads", seen + ", and keeps that snapshot");
            Db.update(b, "update accounts set balance = balance - 30 where id = 'ANA-1'");
            b.commit();
            fact(out, "B", "takes 30 and commits: " + balanceOf(db, "ANA-1"));
            try {
                Db.update(a, "update accounts set balance = balance - 30 where id = 'ANA-1'");
                a.commit();
                fact(out, "A updates", "applied");
            } catch (SQLException e) {
                a.rollback();
                fact(out, "A updates", "refused: " + SqlStates.named(e.getSQLState()));
                fact(out, "message", e.getMessage().replaceAll("^ERROR: ", "").split("\n")[0]);
            }
            int attempts = retrying(() -> {
                a.rollback();
                Db.update(a, "update accounts set balance = balance - 30 where id = 'ANA-1'");
                a.commit();
                return true;
            });
            fact(out, "A retries", "from the start (" + attempts + " attempt): 70 - 30 = " + balanceOf(db, "ANA-1"));
        }
        line(out, "  ONE RULE OVER TWO ROWS (ANA-1 100, ANA-2 100, TOTAL >= 0):");
        fact(out, "A and B", "each read the total, then take 150 from one row");
        for (int level : new int[] {Connection.TRANSACTION_REPEATABLE_READ, Connection.TRANSACTION_SERIALIZABLE}) {
            db.reset();
            boolean serializable = level == Connection.TRANSACTION_SERIALIZABLE;
            line(out, serializable ? "  AFTER THE FIX: SERIALIZABLE:" : "  BEFORE THE FIX: REPEATABLE READ:");
            try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
                a.setTransactionIsolation(level);
                b.setTransactionIsolation(level);
                int sumA = Db.one(a, "select sum(balance) from household");
                int sumB = Db.one(b, "select sum(balance) from household");
                fact(out, "A and B see", "total " + sumA + " and " + sumB + ": room for 150 each");
                Db.update(a, "update household set balance = balance - 150 where id = 'ANA-1'");
                a.commit();
                fact(out, "A", "takes 150 from ANA-1, commits");
                boolean committed;
                try {
                    Db.update(b, "update household set balance = balance - 150 where id = 'ANA-2'");
                    b.commit();
                    committed = true;
                    fact(out, "B", "takes 150 from ANA-2, commits");
                } catch (SQLException e) {
                    b.rollback();
                    committed = false;
                    fact(out, "B", "refused: " + SqlStates.named(e.getSQLState()));
                }
                try (Connection c = db.connect("read")) {
                    int total = Db.one(c, "select sum(balance) from household");
                    fact(out, "total", sumA + " - 150" + (committed ? " - 150" : "") + " = " + total
                            + (total < 0 ? "  <- WRITE SKEW" : ""));
                }
            }
        }
    }

    private static void act4(StringBuilder out, Db db) throws Exception {
        line(out, "FOUR. Optimistic locking with Hibernate, and one-time keys.");
        try (Hib hib = new Hib(db)) {
            try (Session s = hib.factory.openSession()) {
                Transaction t = s.beginTransaction();
                s.persist(new HibAccount("ANA-1", 100));
                t.commit();
            }
            try (Session a = hib.factory.openSession(); Session b = hib.factory.openSession()) {
                Transaction ta = a.beginTransaction();
                Transaction tb = b.beginTransaction();
                HibAccount seenByA = a.find(HibAccount.class, "ANA-1");
                HibAccount seenByB = b.find(HibAccount.class, "ANA-1");
                line(out, "  BOTH LOAD THE ACCOUNT (Hibernate 7, a @Version column):");
                fact(out, "A", "balance " + seenByA.balance() + ", version " + seenByA.version());
                fact(out, "B", "balance " + seenByB.balance() + ", version " + seenByB.version());
                long loaded = seenByA.version();
                boolean paidA = seenByA.withdraw(80);
                boolean paidB = seenByB.withdraw(80);
                hib.sql.clear();
                ta.commit();
                String update = hib.sql.stream().filter(q -> q.startsWith("update")).findFirst().orElse("?");
                int where = update.indexOf(" where");
                line(out, "  A PAYS 80 (" + paidA + ") AND COMMITS:");
                fact(out, "sql", update.substring(0, where));
                fact(out, "", update.substring(where + 1));
                fact(out, "values", "balance " + seenByA.balance() + ", version " + loaded + " + 1 = " + seenByA.version()
                        + ", where version = " + loaded);
                line(out, "  B PAYS 80 (" + paidB + ") ON WHAT IT LOADED, AND COMMITS:");
                fact(out, "where", "version = " + loaded + ", but the row is at " + seenByA.version() + ": 0 rows");
                String second;
                try {
                    tb.commit();
                    second = "committed";
                } catch (Exception e) {
                    tb.rollback();
                    second = e instanceof OptimisticLockException || e.getCause() instanceof OptimisticLockException
                            ? "refused: OptimisticLockException" : "refused: " + e.getClass().getSimpleName();
                }
                fact(out, "commit", second);
            }
            try (Session b = hib.factory.openSession()) {
                Transaction t = b.beginTransaction();
                HibAccount fresh = b.find(HibAccount.class, "ANA-1");
                int balance = fresh.balance();
                boolean paid = fresh.withdraw(80);
                t.commit();
                fact(out, "B retries", "loads " + balance + " at version " + fresh.version() + ", " + balance + " >= 80: " + paid);
            }
            try (Session s = hib.factory.openSession()) {
                Transaction t = s.beginTransaction();
                hib.sql.clear();
                s.find(HibAccount.class, "ANA-1", LockMode.PESSIMISTIC_WRITE);
                line(out, "  A PESSIMISTIC WRITE LOCK FROM HIBERNATE SENDS:");
                fact(out, "sql", hib.sql.stream().filter(q -> q.startsWith("select")).findFirst()
                        .map(q -> "select ... " + q.substring(q.indexOf("for no key update"))).orElse("?"));
                t.commit();
            }
        }
        db.reset();
        line(out, "  ONE PAYMENT KEY, SENT TWICE AT ONCE (KEY IS THE PRIMARY KEY):");
        try (Connection a = db.connect("A")) {
            int rowsA = Db.update(a, CLAIM_KEY);
            Db.update(a, "update accounts set balance = balance - 70, paid_out = paid_out + 70 where id = 'ANA-1'");
            fact(out, "A", "inserts rent-october: " + rowsA + " row, moves 70");
            CompletableFuture<Integer> b = async(() -> {
                try (Connection c = db.connect("B")) {
                    int rows = Db.update(c, CLAIM_KEY);
                    if (rows == 1) {
                        Db.update(c, "update accounts set balance = balance - 70, paid_out = paid_out + 70 where id = 'ANA-1'");
                    }
                    c.commit();
                    return rows;
                }
            });
            boolean waiting = db.waitUntilWaiting("B");
            fact(out, "B", "inserts the same key: waits for A " + waiting);
            a.commit();
            fact(out, "A", "commits the key and the money together");
            fact(out, "B", b.get() + " rows: the key is taken, nothing moves");
        }
        fact(out, "ana", "100 - 70 = " + balanceOf(db, "ANA-1"));
    }

    private static void act5(StringBuilder out, Db db) throws Exception {
        line(out, "FIVE. The bill.");
        db.reset();
        line(out, "  TWO TRANSFERS, EACH LOCKING ITS OWN SENDER FIRST:");
        fact(out, "A", "locks ANA-1, then asks for BEN-2");
        fact(out, "B", "locks BEN-2, then asks for ANA-1");
        CountDownLatch bothHoldOne = new CountDownLatch(2);
        List<String> results = new ArrayList<>();
        CompletableFuture<String> a = async(() -> crossLock(db, "A", "ANA-1", "BEN-2", bothHoldOne));
        CompletableFuture<String> b = async(() -> crossLock(db, "B", "BEN-2", "ANA-1", bothHoldOne));
        results.add(a.get());
        results.add(b.get());
        long cancelled = results.stream().filter(r -> r.equals("40P01")).count();
        fact(out, "PostgreSQL", "finds the cycle, cancels " + cancelled + ": " + SqlStates.named("40P01"));
        fact(out, "the other", (results.contains("done") ? "done" : "?") + "  <- RETRY THE CANCELLED ONE");
        db.reset();
        line(out, "  AFTER THE FIX: ONE LOCK ORDER (ORDER BY ID ... FOR UPDATE):");
        CountDownLatch none = new CountDownLatch(0);
        CompletableFuture<String> c = async(() -> orderedLock(db, "A", none));
        CompletableFuture<String> d = async(() -> orderedLock(db, "B", none));
        fact(out, "A and B", c.get() + " and " + d.get());
        db.reset();
        ReentrantLock serverOne = new ReentrantLock();
        ReentrantLock serverTwo = new ReentrantLock();
        try (Connection one = db.connect("A"); Connection two = db.connect("B")) {
            serverOne.lock();
            serverTwo.lock();
            int seenOne = balance(one, "ANA-1");
            int seenTwo = balance(two, "ANA-1");
            Db.update(one, "update accounts set balance = ?, paid_out = paid_out + 80 where id = 'ANA-1'", seenOne - 80);
            one.commit();
            Db.update(two, "update accounts set balance = ?, paid_out = paid_out + 80 where id = 'ANA-1'", seenTwo - 80);
            two.commit();
            serverOne.unlock();
            serverTwo.unlock();
        }
        line(out, "  TWO SERVERS, EACH WITH A LOCK IN ITS OWN MEMORY:");
        fact(out, "paid out", "80 + 80 = " + paidOut(db) + " from 100  <- THE LOCKS NEVER MEET");
        db.reset();
        line(out, "  A ROW LOCKED WHILE A PERSON CONFIRMS:");
        try (Connection idle = db.connect("A"); Connection waiter = db.connect("B")) {
            Db.one(idle, "select balance from accounts where id = 'ANA-1' for update");
            waiter.createStatement().execute("set lock_timeout = '300ms'");
            String result;
            try {
                Db.one(waiter, "select balance from accounts where id = 'ANA-1' for update");
                result = "got the row";
            } catch (SQLException e) {
                waiter.rollback();
                result = "gave up after 300 ms: " + e.getSQLState();
            }
            fact(out, "B", result + "  <- KEEP IT SHORT");
        }
        line(out, "  WHAT EACH ERROR CODE ASKS OF THE APPLICATION:");
        for (Map.Entry<String, SqlStates.Meaning> e : SqlStates.KNOWN.entrySet()) {
            line(out, String.format("    %s  %-23s%s", e.getKey(), e.getValue().name(), e.getValue().action()));
        }
    }

    private static String crossLock(Db db, String name, String first, String second, CountDownLatch bothHoldOne) throws Exception {
        try (Connection c = db.connect(name)) {
            Db.one(c, "select balance from accounts where id = ? for update", first);
            bothHoldOne.countDown();
            bothHoldOne.await();
            Db.one(c, "select balance from accounts where id = ? for update", second);
            c.commit();
            return "done";
        } catch (SQLException e) {
            return e.getSQLState();
        }
    }

    private static String orderedLock(Db db, String name, CountDownLatch go) throws Exception {
        try (Connection c = db.connect(name)) {
            go.await();
            Db.one(c, "select count(*) from (select id from accounts where id in ('ANA-1', 'BEN-2') order by id for update) t");
            Thread.sleep(300);
            c.commit();
            return "done";
        } catch (SQLException e) {
            return e.getSQLState();
        }
    }
}
