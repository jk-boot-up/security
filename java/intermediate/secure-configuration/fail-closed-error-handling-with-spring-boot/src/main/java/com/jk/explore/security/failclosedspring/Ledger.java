package com.jk.explore.security.failclosedspring;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The accounts table in an in-memory H2 database. Each statement commits on its own unless a transaction is open around it. */
@Repository
class Ledger {

    private final JdbcClient jdbc;

    Ledger(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    long cents(String account) {
        return jdbc.sql("SELECT cents FROM accounts WHERE id = :id").param("id", account).query(Long.class).single();
    }

    long total() {
        return jdbc.sql("SELECT SUM(cents) FROM accounts").query(Long.class).single();
    }

    void debit(String account, long cents) {
        jdbc.sql("UPDATE accounts SET cents = cents - :c WHERE id = :id").param("c", cents).param("id", account).update();
    }

    void credit(String account, long cents) throws FrozenAccountException {
        boolean frozen = jdbc.sql("SELECT frozen FROM accounts WHERE id = :id").param("id", account).query(Boolean.class).single();
        if (frozen) {
            throw new FrozenAccountException(account);
        }
        jdbc.sql("UPDATE accounts SET cents = cents + :c WHERE id = :id").param("c", cents).param("id", account).update();
    }

    void reset() {
        jdbc.sql("UPDATE accounts SET cents = 100000 WHERE id = 'ana'").update();
        jdbc.sql("UPDATE accounts SET cents = 0 WHERE id = 'ben'").update();
    }
}
