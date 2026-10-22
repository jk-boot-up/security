package com.jk.explore.security.racepg;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class RaceConditionsDemo {

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
        db.reset();
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int seenA = balance(a, "ANA-1");
            int seenB = balance(b, "ANA-1");
            boolean checkA = seenA >= 80;
            boolean checkB = seenB >= 80;
            Db.update(a, "update accounts set balance = ?, paid_out = paid_out + 80 where id = 'ANA-1'", seenA - 80);
            a.commit();
            Db.update(b, "update accounts set balance = ?, paid_out = paid_out + 80 where id = 'ANA-1'", seenB - 80);
            b.commit();
            line(out, "  ana has 100 and sends two withdrawals of 80; A reads " + seenA + ", B reads " + seenB + ", both checks pass (" + checkA + ", " + checkB + "), both write the balance they worked out");
        }
        line(out, "  the bank paid out " + paidOut(db) + " from a balance of 100, and the balance says " + balanceOf(db, "ANA-1") + "  <- BOTH CHECKS PASSED BEFORE EITHER WROTE");
        db.reset();
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            Db.update(a, "update accounts set balance = balance - 80, paid_out = paid_out + 80 where id = 'ANA-1'");
            a.commit();
            String second;
            try {
                Db.update(b, "update accounts set balance = balance - 80, paid_out = paid_out + 80 where id = 'ANA-1'");
                b.commit();
                second = "paid";
            } catch (SQLException e) {
                b.rollback();
                second = "refused by the database (" + e.getSQLState() + ", check_violation)";
            }
            line(out, "  the same two with the rule in the table, balance int check (balance >= 0), and the update relative to what is there: A paid 80, B " + second + "; paid out " + paidOut(db) + ", balance " + balanceOf(db, "ANA-1"));
        }
        db.reset();
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int rowsA = Db.update(a, "update accounts set balance = balance - 80, paid_out = paid_out + 80 where id = 'ANA-1' and balance >= 80");
            a.commit();
            int rowsB = Db.update(b, "update accounts set balance = balance - 80, paid_out = paid_out + 80 where id = 'ANA-1' and balance >= 80");
            b.commit();
            line(out, "  or with the check inside the update, where balance >= 80: A changed " + rowsA + " row, B changed " + rowsB + " rows, so B knows it was refused; paid out " + paidOut(db));
        }
        db.reset();
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int usedA = Db.one(a, "select case when used then 1 else 0 end from coupons where code = 'SPRING20'");
            int usedB = Db.one(b, "select case when used then 1 else 0 end from coupons where code = 'SPRING20'");
            for (Connection c : List.of(a, b)) {
                Db.update(c, "update coupons set used = true, discount_given = discount_given + 20 where code = 'SPRING20'");
                c.commit();
            }
            line(out, "  a single-use coupon checked, applied, then marked used, by two requests that both saw used = " + (usedA == 1) + " and " + (usedB == 1) + ": discount given " + Db.one(a, "select discount_given from coupons"));
        }
        db.reset();
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int claimA = Db.update(a, "update coupons set used = true where code = 'SPRING20' and used = false");
            a.commit();
            int claimB = Db.update(b, "update coupons set used = true where code = 'SPRING20' and used = false");
            b.commit();
            line(out, "  the coupon claimed in one statement, where used = false, before the discount is given: A claimed " + claimA + ", B claimed " + claimB);
        }
        line(out, "  the gap between a read and a write is where two requests meet; the database can close it, but only if it is told to");
    }

    private static void act2(StringBuilder out, Db db) throws Exception {
        line(out, "TWO. Row locks: making the check and the write one step.");
        db.reset();
        try (Connection a = db.connect("A")) {
            int seen = Db.one(a, "select balance from accounts where id = 'ANA-1' for update");
            CompletableFuture<String> b = async(() -> {
                try (Connection c = db.connect("B")) {
                    int read = Db.one(c, "select balance from accounts where id = 'ANA-1' for update");
                    boolean paid = read >= 80;
                    if (paid) {
                        Db.update(c, "update accounts set balance = balance - 80, paid_out = paid_out + 80 where id = 'ANA-1'");
                    }
                    c.commit();
                    return "read " + read + ", " + (paid ? "paid out 80" : "refused");
                }
            });
            boolean waiting = db.waitUntilWaiting("B");
            line(out, "  A selects the account for update and reads " + seen + "; B asks for the same row and waits (pg_stat_activity shows a Lock wait): " + waiting);
            Db.update(a, "update accounts set balance = balance - 80, paid_out = paid_out + 80 where id = 'ANA-1'");
            a.commit();
            line(out, "  A pays out 80 and commits; then B: " + b.get() + "; paid out " + paidOut(db) + ", balance " + balanceOf(db, "ANA-1"));
        }
        db.reset();
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            Db.one(a, "select balance from accounts where id = 'ANA-1' for update");
            String nowait;
            try {
                Db.one(b, "select balance from accounts where id = 'ANA-1' for update nowait");
                nowait = "got the row";
            } catch (SQLException e) {
                b.rollback();
                nowait = "refused at once (" + e.getSQLState() + ", lock_not_available)";
            }
            line(out, "  with NOWAIT, B does not wait: " + nowait);
            b.createStatement().execute("set lock_timeout = '300ms'");
            long start = System.nanoTime();
            String timeout;
            try {
                Db.one(b, "select balance from accounts where id = 'ANA-1' for update");
                timeout = "got the row";
            } catch (SQLException e) {
                b.rollback();
                timeout = "gave up (" + e.getSQLState() + ")";
            }
            line(out, "  with lock_timeout of 300 ms, B waits and then: " + timeout + ", after at least 300 ms: " + ((System.nanoTime() - start) / 1_000_000 >= 300));
            int other = Db.one(b, "select count(*) from (select id from accounts order by id for update skip locked limit 1) t where id = 'BEN-2'");
            line(out, "  with SKIP LOCKED, B takes the next row that nobody holds, as workers do on a queue: BEN-2 taken " + (other == 1));
        }
        line(out, "  the lock turns check-then-write into a queue: the second request runs after the first, and sees what it did");
    }

    /** Runs {@code work} until it succeeds or fails with something other than a serialization failure or a deadlock; returns the number of attempts. */
    static int retrying(Db db, String name, int isolation, Callable<Boolean> work) throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                work.call();
                return attempt;
            } catch (SQLException e) {
                if (!e.getSQLState().equals("40001") && !e.getSQLState().equals("40P01")) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("gave up after 5 attempts");
    }

    private static void act3(StringBuilder out, Db db) throws Exception {
        line(out, "THREE. Isolation levels.");
        db.reset();
        db.exec("update accounts set balance = 100");
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int seen = balance(a, "ANA-1");
            Db.update(b, "update accounts set balance = balance - 30 where id = 'ANA-1'");
            b.commit();
            Db.update(a, "update accounts set balance = balance - 30 where id = 'ANA-1'");
            a.commit();
            line(out, "  READ COMMITTED (the default): A read " + seen + ", B took 30 and committed, A's update of balance - 30 was applied to the new value: balance " + balanceOf(db, "ANA-1"));
        }
        db.reset();
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            a.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            int seen = balance(a, "ANA-1");
            Db.update(b, "update accounts set balance = balance - 30 where id = 'ANA-1'");
            b.commit();
            String result;
            try {
                Db.update(a, "update accounts set balance = balance - 30 where id = 'ANA-1'");
                a.commit();
                result = "applied";
            } catch (SQLException e) {
                a.rollback();
                result = "refused (" + e.getSQLState() + ", serialization_failure: " + e.getMessage().replaceAll("^ERROR: ", "").split("\n")[0] + ")";
            }
            line(out, "  REPEATABLE READ: A read " + seen + " and keeps that snapshot; B took 30 and committed; A's update: " + result);
            retrying(db, "A", Connection.TRANSACTION_REPEATABLE_READ, () -> {
                a.rollback();
                Db.update(a, "update accounts set balance = balance - 30 where id = 'ANA-1'");
                a.commit();
                return true;
            });
            line(out, "  A starts again from the beginning and succeeds: balance " + balanceOf(db, "ANA-1") + "  <- THE CALLER MUST BE ABLE TO RETRY");
        }
        for (int level : new int[] {Connection.TRANSACTION_REPEATABLE_READ, Connection.TRANSACTION_SERIALIZABLE}) {
            db.reset();
            String name = level == Connection.TRANSACTION_SERIALIZABLE ? "SERIALIZABLE" : "REPEATABLE READ";
            try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
                a.setTransactionIsolation(level);
                b.setTransactionIsolation(level);
                int sumA = Db.one(a, "select sum(balance) from household");
                int sumB = Db.one(b, "select sum(balance) from household");
                Db.update(a, "update household set balance = balance - 150 where id = 'ANA-1'");
                a.commit();
                String second;
                try {
                    Db.update(b, "update household set balance = balance - 150 where id = 'ANA-2'");
                    b.commit();
                    second = "commits too";
                } catch (SQLException e) {
                    b.rollback();
                    second = "is refused (" + e.getSQLState() + ", serialization_failure)";
                }
                try (Connection c = db.connect("read")) {
                    line(out, "  " + name + ", two accounts that must stay at 0 or more in total: A and B both see " + sumA + " and " + sumB + ", each takes 150 from a different account; A commits, B " + second + "; total " + Db.one(c, "select sum(balance) from household")
                            + (level == Connection.TRANSACTION_SERIALIZABLE ? "" : "  <- WRITE SKEW: NEITHER UPDATE TOUCHED THE OTHER'S ROW"));
                }
            }
        }
        line(out, "  a stronger level makes the database refuse some interleavings, and the application must retry what it refuses");
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
                line(out, "  A and B both load the account: balance " + seenByA.balance() + " and " + seenByB.balance() + ", version " + seenByA.version() + " and " + seenByB.version());
                boolean paidA = seenByA.withdraw(80);
                boolean paidB = seenByB.withdraw(80);
                hib.sql.clear();
                ta.commit();
                line(out, "  A pays 80 (" + paidA + ") and commits with: " + hib.sql.stream().filter(q -> q.startsWith("update")).findFirst().orElse("?"));
                String second;
                try {
                    tb.commit();
                    second = "committed";
                } catch (Exception e) {
                    tb.rollback();
                    second = (e instanceof OptimisticLockException || e.getCause() instanceof OptimisticLockException ? "refused (OptimisticLockException: the row is not at the version B loaded)" : "refused (" + e.getClass().getSimpleName() + ")");
                }
                line(out, "  B pays 80 (" + paidB + ") on what it loaded, and commits: " + second);
            }
            try (Session b = hib.factory.openSession()) {
                Transaction t = b.beginTransaction();
                HibAccount fresh = b.find(HibAccount.class, "ANA-1");
                boolean paid = fresh.withdraw(80);
                t.commit();
                line(out, "  B starts again, loads balance " + fresh.balance() + " at version " + fresh.version() + ", and is refused by the rule: paid " + paid);
            }
            try (Session s = hib.factory.openSession()) {
                Transaction t = s.beginTransaction();
                hib.sql.clear();
                s.find(HibAccount.class, "ANA-1", LockMode.PESSIMISTIC_WRITE);
                line(out, "  asking Hibernate for a pessimistic write lock sends: " + hib.sql.stream().filter(q -> q.startsWith("select")).findFirst().map(q -> "select ... " + q.substring(q.indexOf("for no key update"))).orElse("?"));
                t.commit();
            }
        }
        db.reset();
        try (Connection a = db.connect("A")) {
            int rowsA = Db.update(a, "insert into payments (key, amount) values ('rent-october', 70) on conflict do nothing");
            Db.update(a, "update accounts set balance = balance - 70, paid_out = paid_out + 70 where id = 'ANA-1'");
            CompletableFuture<Integer> b = async(() -> {
                try (Connection c = db.connect("B")) {
                    int rows = Db.update(c, "insert into payments (key, amount) values ('rent-october', 70) on conflict do nothing");
                    if (rows == 1) {
                        Db.update(c, "update accounts set balance = balance - 70, paid_out = paid_out + 70 where id = 'ANA-1'");
                    }
                    c.commit();
                    return rows;
                }
            });
            boolean waiting = db.waitUntilWaiting("B");
            a.commit();
            line(out, "  the same payment key arriving twice at once: A claims the key and moves 70 in one transaction (rows " + rowsA + "); B, with the same key, waits for A (" + waiting + ") and then finds the key taken (rows " + b.get() + "); ana has " + balanceOf(db, "ANA-1"));
        }
        line(out, "  the unique key and the money move share a transaction, so a key is never claimed without the payment, or the payment made without the key");
    }

    private static void act5(StringBuilder out, Db db) throws Exception {
        line(out, "FIVE. The bill.");
        db.reset();
        CountDownLatch bothHoldOne = new CountDownLatch(2);
        List<String> results = new ArrayList<>();
        CompletableFuture<String> a = async(() -> crossLock(db, "A", "ANA-1", "BEN-2", bothHoldOne));
        CompletableFuture<String> b = async(() -> crossLock(db, "B", "BEN-2", "ANA-1", bothHoldOne));
        results.add(a.get());
        results.add(b.get());
        long cancelled = results.stream().filter(r -> r.equals("deadlock")).count();
        line(out, "  two transfers in opposite directions, each locking its own sender first: PostgreSQL finds the cycle and cancels exactly one (40P01): " + (cancelled == 1) + ", the other finished: " + results.contains("done") + "  <- A RETRY IS PART OF THE DESIGN");
        db.reset();
        CountDownLatch none = new CountDownLatch(0);
        CompletableFuture<String> c = async(() -> orderedLock(db, "A", none));
        CompletableFuture<String> d = async(() -> orderedLock(db, "B", none));
        line(out, "  the same two with every lock taken in one order, order by id: " + c.get() + " and " + d.get());
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
        line(out, "  two servers, each with a lock of its own in its own memory: paid out " + paidOut(db) + " from 100  <- A LOCK IN ONE PROCESS DOES NOT STOP ANOTHER");
        db.reset();
        try (Connection idle = db.connect("A"); Connection waiter = db.connect("B")) {
            Db.one(idle, "select balance from accounts where id = 'ANA-1' for update");
            waiter.createStatement().execute("set lock_timeout = '300ms'");
            String result;
            try {
                Db.one(waiter, "select balance from accounts where id = 'ANA-1' for update");
                result = "got the row";
            } catch (SQLException e) {
                waiter.rollback();
                result = "gave up (" + e.getSQLState() + ")";
            }
            line(out, "  a request that locks the account and then waits for a person to confirm keeps everyone else waiting: B " + result);
        }
        line(out, "  so: keep transactions short, lock in one order, retry on 40001 and 40P01, and let the database enforce the rules that must always hold");
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
            return e.getSQLState().equals("40P01") ? "deadlock" : e.getSQLState();
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
