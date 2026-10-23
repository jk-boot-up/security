package com.jk.explore.security.privacyeng;

import java.sql.SQLException;
import java.time.LocalDate;

/** How long each kind of data may be kept, and the job that removes what has expired, except where a legal hold applies. */
public final class Retention {

    public static final int TRANSACTION_YEARS = 7;
    public static final int CONSENT_YEARS = 2;
    public static final int NOTE_YEARS = 1;

    private Retention() {
    }

    /** What the job would do on the given day, as counts. */
    public record Plan(long expiredTransactions, long underHold, long expiredConsents, long expiredNotes) {
    }

    public static Plan plan(Pg pg, LocalDate today) throws SQLException {
        long expired = pg.count("SELECT count(*) FROM transactions WHERE booked_on < ?", java.sql.Date.valueOf(today.minusYears(TRANSACTION_YEARS)));
        long hold = pg.count("SELECT count(*) FROM transactions t JOIN accounts a ON a.id = t.account_id JOIN customers c ON c.id = a.customer_id WHERE t.booked_on < ? AND c.legal_hold", java.sql.Date.valueOf(today.minusYears(TRANSACTION_YEARS)));
        long consents = pg.count("SELECT count(*) FROM customers WHERE marketing_consent AND consent_date < ?", java.sql.Date.valueOf(today.minusYears(CONSENT_YEARS)));
        long notes = pg.count("SELECT count(*) FROM support_notes WHERE created_on < ?", java.sql.Date.valueOf(today.minusYears(NOTE_YEARS)));
        return new Plan(expired, hold, consents, notes);
    }

    /** Runs the job; returns what it removed. */
    public static Plan run(Pg pg, LocalDate today) throws SQLException {
        Plan plan = plan(pg, today);
        pg.update("DELETE FROM transactions t USING accounts a, customers c WHERE a.id = t.account_id AND c.id = a.customer_id AND NOT c.legal_hold AND t.booked_on < ?", java.sql.Date.valueOf(today.minusYears(TRANSACTION_YEARS)));
        pg.update("UPDATE customers SET marketing_consent = FALSE, consent_date = NULL WHERE marketing_consent AND consent_date < ?", java.sql.Date.valueOf(today.minusYears(CONSENT_YEARS)));
        pg.update("DELETE FROM support_notes WHERE created_on < ?", java.sql.Date.valueOf(today.minusYears(NOTE_YEARS)));
        return plan;
    }
}
