package com.jk.explore.security.racepg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.OptimisticLockException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** A real PostgreSQL 18 in Docker. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RaceConditionsTest {

    private Db db;

    @BeforeAll
    void start() throws Exception {
        db = new Db();
    }

    @AfterAll
    void stop() {
        db.close();
    }

    @BeforeEach
    void reset() throws Exception {
        db.reset();
    }

    @Test
    void readThenWriteLetsBothWithdrawalsThrough() throws Exception {
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            int seenA = RaceConditionsDemo.balance(a, "ANA-1");
            int seenB = RaceConditionsDemo.balance(b, "ANA-1");
            Db.update(a, "update accounts set balance = ?, paid_out = paid_out + 80 where id = 'ANA-1'", seenA - 80);
            a.commit();
            Db.update(b, "update accounts set balance = ?, paid_out = paid_out + 80 where id = 'ANA-1'", seenB - 80);
            b.commit();
        }
        assertEquals(160, RaceConditionsDemo.paidOut(db));
        assertEquals(20, RaceConditionsDemo.balanceOf(db, "ANA-1"));
    }

    @Test
    void theCheckConstraintAndTheConditionalUpdateRefuseTheSecondWithdrawal() throws Exception {
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            Db.update(a, "update accounts set balance = balance - 80 where id = 'ANA-1'");
            a.commit();
            SQLException e = assertThrows(SQLException.class, () -> Db.update(b, "update accounts set balance = balance - 80 where id = 'ANA-1'"));
            assertEquals("23514", e.getSQLState());
            b.rollback();
            assertEquals(0, Db.update(b, "update accounts set balance = balance - 80 where id = 'ANA-1' and balance >= 80"));
        }
    }

    @Test
    void aCouponClaimedInOneStatementIsClaimedOnce() throws Exception {
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            assertEquals(1, Db.update(a, "update coupons set used = true where code = 'SPRING20' and used = false"));
            a.commit();
            assertEquals(0, Db.update(b, "update coupons set used = true where code = 'SPRING20' and used = false"));
        }
    }

    @Test
    void aRowLockMakesTheSecondRequestWaitAndThenSeeTheFirst() throws Exception {
        try (Connection a = db.connect("A")) {
            Db.one(a, "select balance from accounts where id = 'ANA-1' for update");
            CompletableFuture<Integer> b = RaceConditionsDemo.async(() -> {
                try (Connection c = db.connect("B")) {
                    int read = Db.one(c, "select balance from accounts where id = 'ANA-1' for update");
                    c.commit();
                    return read;
                }
            });
            assertTrue(db.waitUntilWaiting("B"));
            Db.update(a, "update accounts set balance = balance - 80 where id = 'ANA-1'");
            a.commit();
            assertEquals(20, b.get());
        }
    }

    @Test
    void nowaitAndLockTimeoutRefuseInsteadOfWaiting() throws Exception {
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            Db.one(a, "select balance from accounts where id = 'ANA-1' for update");
            assertEquals("55P03", assertThrows(SQLException.class, () -> Db.one(b, "select balance from accounts where id = 'ANA-1' for update nowait")).getSQLState());
            b.rollback();
            b.createStatement().execute("set lock_timeout = '200ms'");
            assertEquals("55P03", assertThrows(SQLException.class, () -> Db.one(b, "select balance from accounts where id = 'ANA-1' for update")).getSQLState());
        }
    }

    @Test
    void repeatableReadRefusesAnUpdateOfARowThatChangedSinceTheSnapshot() throws Exception {
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            a.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            RaceConditionsDemo.balance(a, "ANA-1");
            Db.update(b, "update accounts set balance = balance - 30 where id = 'ANA-1'");
            b.commit();
            assertEquals("40001", assertThrows(SQLException.class, () -> Db.update(a, "update accounts set balance = balance - 30 where id = 'ANA-1'")).getSQLState());
        }
    }

    @Test
    void onlySerializableStopsWriteSkew() throws Exception {
        assertEquals(-100, writeSkew(Connection.TRANSACTION_REPEATABLE_READ));
        db.reset();
        assertEquals(50, writeSkew(Connection.TRANSACTION_SERIALIZABLE));
    }

    private int writeSkew(int level) throws Exception {
        try (Connection a = db.connect("A"); Connection b = db.connect("B")) {
            a.setTransactionIsolation(level);
            b.setTransactionIsolation(level);
            Db.one(a, "select sum(balance) from household");
            Db.one(b, "select sum(balance) from household");
            Db.update(a, "update household set balance = balance - 150 where id = 'ANA-1'");
            a.commit();
            try {
                Db.update(b, "update household set balance = balance - 150 where id = 'ANA-2'");
                b.commit();
            } catch (SQLException e) {
                assertEquals("40001", e.getSQLState());
                b.rollback();
            }
        }
        try (Connection c = db.connect("read")) {
            return Db.one(c, "select sum(balance) from household");
        }
    }

    @Test
    void hibernateRefusesTheSecondWriterOfAVersionedRow() throws Exception {
        try (Hib hib = new Hib(db)) {
            try (Session s = hib.factory.openSession()) {
                Transaction t = s.beginTransaction();
                s.persist(new HibAccount("ANA-9", 100));
                t.commit();
            }
            try (Session a = hib.factory.openSession(); Session b = hib.factory.openSession()) {
                Transaction ta = a.beginTransaction();
                Transaction tb = b.beginTransaction();
                HibAccount seenByA = a.find(HibAccount.class, "ANA-9");
                HibAccount seenByB = b.find(HibAccount.class, "ANA-9");
                assertTrue(seenByA.withdraw(80));
                assertTrue(seenByB.withdraw(80));
                hib.sql.clear();
                ta.commit();
                assertTrue(hib.sql.stream().anyMatch(q -> q.startsWith("update hib_account") && q.contains("version=?") && q.contains("and version=?")));
                Exception e = assertThrows(Exception.class, tb::commit);
                assertTrue(e instanceof OptimisticLockException || e.getCause() instanceof OptimisticLockException, e.toString());
                tb.rollback();
            }
            try (Session b = hib.factory.openSession()) {
                Transaction t = b.beginTransaction();
                HibAccount fresh = b.find(HibAccount.class, "ANA-9");
                assertEquals(20, fresh.balance());
                assertEquals(1, fresh.version());
                assertFalse(fresh.withdraw(80));
                t.commit();
            }
        }
    }

    @Test
    void aOneTimeKeyClaimedInTheSameTransactionAsTheMoneyRunsOnce() throws Exception {
        try (Connection a = db.connect("A")) {
            assertEquals(1, Db.update(a, "insert into payments (key, amount) values ('rent', 70) on conflict do nothing"));
            Db.update(a, "update accounts set balance = balance - 70 where id = 'ANA-1'");
            CompletableFuture<Integer> b = RaceConditionsDemo.async(() -> {
                try (Connection c = db.connect("B")) {
                    int rows = Db.update(c, "insert into payments (key, amount) values ('rent', 70) on conflict do nothing");
                    c.commit();
                    return rows;
                }
            });
            assertTrue(db.waitUntilWaiting("B"));
            a.commit();
            assertEquals(0, b.get());
            assertEquals(30, RaceConditionsDemo.balanceOf(db, "ANA-1"));
        }
    }

    @Test
    void oppositeLockOrderDeadlocksAndTheDatabaseCancelsOne() throws Exception {
        java.util.concurrent.CountDownLatch both = new java.util.concurrent.CountDownLatch(2);
        CompletableFuture<String> a = RaceConditionsDemo.async(() -> cross("A", "ANA-1", "BEN-2", both));
        CompletableFuture<String> b = RaceConditionsDemo.async(() -> cross("B", "BEN-2", "ANA-1", both));
        java.util.List<String> results = java.util.List.of(a.get(), b.get());
        assertEquals(1, results.stream().filter(r -> r.equals("40P01")).count());
        assertTrue(results.contains("done"));
    }

    private String cross(String name, String first, String second, java.util.concurrent.CountDownLatch both) throws Exception {
        try (Connection c = db.connect(name)) {
            Db.one(c, "select balance from accounts where id = ? for update", first);
            both.countDown();
            both.await();
            Db.one(c, "select balance from accounts where id = ? for update", second);
            c.commit();
            return "done";
        } catch (SQLException e) {
            return e.getSQLState();
        }
    }
}
