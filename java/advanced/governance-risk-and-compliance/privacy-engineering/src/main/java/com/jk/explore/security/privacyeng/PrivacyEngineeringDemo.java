package com.jk.explore.security.privacyeng;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Minimizes, pseudonymizes, retains and erases a bank's customer data in PostgreSQL, and measures what is left. */
public class PrivacyEngineeringDemo {

    static final int CUSTOMER = 17;
    static final int OUTSIDE_SHARE = 7;     // the outside list holds customers whose id ends in 0 to 6: 70%

    private final StringBuilder out = new StringBuilder();
    private final Pg pg;

    private PrivacyEngineeringDemo(Pg pg) {
        this.pg = pg;
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String pct(double x) {
        return String.format("%.1f%%", x * 100);
    }

    public static String run() throws Exception {
        try (Pg pg = new Pg()) {
            Seed.create(pg);
            PrivacyEngineeringDemo demo = new PrivacyEngineeringDemo(pg);
            demo.collected();
            demo.pickedOut();
            Pseudonyms pseudonyms = new Pseudonyms("analytics-key-one");
            demo.pseudonyms(pseudonyms);
            demo.retention();
            demo.request(pseudonyms);
            return demo.out.toString();
        }
    }

    private void act(String number, String title) {
        out.append(number).append(". ").append(title).append(".\n");
    }

    private void section(String header) {
        out.append("  ").append(header).append(":\n");
    }

    private void line(String format, Object... args) {
        out.append("    ").append(String.format(format, args).stripTrailing()).append("\n");
    }

    private long count(String sql, Object... params) throws Exception {
        return pg.count(sql, params);
    }

    private String value(String column) throws Exception {
        return pg.strings("SELECT " + column + "::text FROM customers WHERE id = ?", CUSTOMER).get(0);
    }

    private void collected() throws Exception {
        act("ONE", "What is collected, and why");
        section("THE DATABASE (made up, seeded: the same on every run)");
        for (String t : List.of("customers", "accounts", "transactions", "support_notes")) {
            line("%-16s%d rows", t, count("SELECT count(*) FROM " + t));
        }
        section("THE " + Purposes.ALL.size() + " PURPOSES");
        for (Purposes.Purpose p : Purposes.ALL) {
            line("%-7s%s", p.code(), p.name());
        }
        section("WHICH PURPOSE NEEDS WHICH PERSONAL COLUMN");
        StringBuilder head = new StringBuilder(String.format("%-19s", "column"));
        for (Purposes.Purpose p : Purposes.ALL) {
            head.append(String.format("%-7s", p.code()));
        }
        line("%spurposes", head);
        for (String column : Purposes.PERSONAL_COLUMNS) {
            StringBuilder row = new StringBuilder(String.format("%-19s", column));
            int n = 0;
            for (Purposes.Purpose p : Purposes.ALL) {
                boolean needs = p.needs().contains(column);
                n += needs ? 1 : 0;
                row.append(String.format("%-7s", needs ? "x" : "."));
            }
            line("%s%d", row, n);
        }
        section("THE RESULT");
        line("needed: %d of %d; for no purpose: %s", Purposes.needed().size(), Purposes.PERSONAL_COLUMNS.size(), String.join(", ", Purposes.withoutPurpose()));
    }

    private void pickedOut() throws Exception {
        act("TWO", "Who can be picked out of data that has no names");
        section("THE VALUES LEFT WITHOUT A NAME (customer " + CUSTOMER + ")");
        LocalDate born = LocalDate.parse(value("birth_date"));
        line("%-12s%s", "postcode", value("postcode"));
        line("%-12s%s", "birth date", born);
        line("%-12s%s", "gender", value("gender"));
        section("HOW THE VALUES ARE MADE COARSE");
        line("%-12s%-12s-> %s", "postcode", value("postcode"), Coarse.postcode(value("postcode")));
        line("%-12s%-12s-> %s", "birth date", born, Coarse.decade(born));
        line("%-12s%-12s-> %s", "gender", value("gender"), value("gender"));
        section("HOW MANY GROUPS THERE CAN BE");
        line("full:   %d postcodes x %d birth dates x %d = %d", Anonymity.distinct(pg, "postcode"), Anonymity.distinct(pg, "birth_date"), Anonymity.distinct(pg, "gender"),
                Anonymity.distinct(pg, "postcode") * Anonymity.distinct(pg, "birth_date") * Anonymity.distinct(pg, "gender"));
        line("coarse: %d prefixes x %d decades x %d = %d", Anonymity.distinct(pg, "left(postcode, 3)"), Anonymity.distinct(pg, "extract(year from birth_date)::int / 10"),
                Anonymity.distinct(pg, "gender"), Anonymity.distinct(pg, "left(postcode, 3)") * Anonymity.distinct(pg, "extract(year from birth_date)::int / 10") * Anonymity.distinct(pg, "gender"));
        line("for %d customers", count("SELECT count(*) FROM customers"));
        section("THE GROUPS (customers alike on the three values)");
        line("%-10s%-9s%-10s%-10s%s", "values", "alone", "under 5", "smallest", "customer " + CUSTOMER + "'s");
        for (String[] v : List.of(new String[] {"full", Anonymity.FULL}, new String[] {"coarse", Anonymity.COARSE})) {
            line("%-10s%-9s%-10s%-10d%d", v[0], pct(Anonymity.shareBelow(pg, "customers", v[1], 2)), pct(Anonymity.shareBelow(pg, "customers", v[1], 5)),
                    Anonymity.smallest(pg, "customers", v[1]), Anonymity.groupOf(pg, v[1], CUSTOMER));
        }
        section("THE LINKING ATTACK (an outside list: name and the three values)");
        line("%-32s%d", "customers on the outside list", count("SELECT count(*) FROM customers WHERE id % 10 < ?", OUTSIDE_SHARE));
        line("%-32s%d", "named with the full values", Anonymity.namedFromOutside(pg, OUTSIDE_SHARE));
        line("%-32s%d", "named with the coarse values", Anonymity.namedFromOutsideCoarse(pg, OUTSIDE_SHARE));
    }

    private void pseudonyms(Pseudonyms pseudonyms) throws Exception {
        act("THREE", "Pseudonyms for analytics");
        PseudonymSteps one = PseudonymSteps.of("analytics-key-one", CUSTOMER);
        PseudonymSteps two = PseudonymSteps.of("analytics-key-two", CUSTOMER);
        section("THE INPUT");
        line("%-8s\"%s\"", "text", one.input());
        line("%-8s%s", "key", "analytics-key-one (kept by the bank)");
        section("HOW THE PSEUDONYM IS MADE");
        line("HMAC-SHA256(key, text), 32 bytes:");
        line("  %s", one.fullMac().substring(0, 32));
        line("  %s", one.fullMac().substring(32));
        line("pseudonym = the first 8 bytes: %s", one.pseudonym());
        section("THE SAME CUSTOMER, AGAIN AND UNDER ANOTHER KEY");
        line("%-20s%s", "analytics-key-one", pseudonyms.of(CUSTOMER));
        line("%-20s%s  %s", "analytics-key-one", new Pseudonyms("analytics-key-one").of(CUSTOMER), "the same");
        line("%-20s%s  %s", "analytics-key-two", two.pseudonym(), "different");
        int rows = pseudonyms.publish(pg);
        section("THE PUBLISHED ANALYTICS ROWS");
        line("%-26s%d", "rows", rows);
        line("%-26s%d", "distinct pseudonyms", count("SELECT count(DISTINCT pseudonym) FROM analytics_tx"));
        line("one row of customer %d:", CUSTOMER);
        line("  %s", pg.strings("SELECT pseudonym || ', ' || amount_band || ', ' || postcode || ', ' || birth_date || ', ' || gender || ', ' || booked_year"
                + " FROM analytics_tx WHERE pseudonym = ? ORDER BY booked_year, amount_band LIMIT 1", pseudonyms.of(CUSTOMER)).get(0));
        section("THE LINKING ATTACK ON THE ROWS");
        line("rows still carry postcode, birth date and gender;");
        line("rows tied back to a name by the outside list: %d of %d", Anonymity.analyticsRowsNamed(pg, OUTSIDE_SHARE), rows);
    }

    private void retention() throws Exception {
        LocalDate today = Seed.TODAY;
        act("FOUR", "Retention, run on " + today);
        section("THE RULES");
        line("%-20s%-10s%s", "data", "kept for", "removed if before");
        line("%-20s%-10s%s", "transactions", Retention.TRANSACTION_YEARS + " years", today.minusYears(Retention.TRANSACTION_YEARS));
        line("%-20s%-10s%s", "marketing consent", Retention.CONSENT_YEARS + " years", today.minusYears(Retention.CONSENT_YEARS));
        line("%-20s%-10s%s", "support notes", Retention.NOTE_YEARS + " year", today.minusYears(Retention.NOTE_YEARS));
        line("legal hold: customers whose id is a multiple of 700");
        Date limit = Date.valueOf(today.minusYears(Retention.TRANSACTION_YEARS));
        long before = count("SELECT count(*) FROM transactions");
        section("BEFORE THE JOB");
        Retention.Plan plan = Retention.plan(pg, today);
        line("%-34s%d", "transactions", before);
        line("%-34s%d (%d under a legal hold)", "transactions older than 7 years", plan.expiredTransactions(), plan.underHold());
        line("%-34s%d", "consents older than 2 years", plan.expiredConsents());
        line("%-34s%d", "support notes older than 1 year", plan.expiredNotes());
        Retention.run(pg, today);
        long left = count("SELECT count(*) FROM transactions");
        section("AFTER THE JOB");
        line("%-34s%d", "transactions deleted", before - left);
        line("%-34s%d = %d - %d", "transactions left", left, before, before - left);
        line("%-34s%d (all under a hold)", "older than 7 years, left", count("SELECT count(*) FROM transactions WHERE booked_on < ?", limit));
        line("%-34s%d", "consents withdrawn", plan.expiredConsents());
        line("%-34s%d", "support notes deleted", plan.expiredNotes());
        section("WHAT THE JOB DID NOT TOUCH");
        line("analytics rows from %d or earlier: %d of %d", today.minusYears(Retention.TRANSACTION_YEARS).getYear() - 1,
                count("SELECT count(*) FROM analytics_tx WHERE booked_year < ?", today.minusYears(Retention.TRANSACTION_YEARS).getYear()),
                count("SELECT count(*) FROM analytics_tx"));
    }

    private void request(Pseudonyms pseudonyms) throws Exception {
        act("FIVE", "A customer asks for their data, then for its erasure");
        Subject subject = new Subject(pg, CUSTOMER);
        List<String> tables = pg.strings("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY 1");
        List<String> map = Subject.dataMap(pg);
        section("THE DATA MAP (found from the foreign keys of the schema)");
        line("%-12s%d in the schema", "tables", tables.size());
        line("%-12s%s", "found", String.join(", ", map));
        line("%-12s%s", "not found", String.join(", ", tables.stream().filter(t -> !map.contains(t)).toList()) + " (no foreign key)");
        section("THE EXPORT (customer " + CUSTOMER + ", " + subject.name() + ")");
        Map<String, Long> export = subject.export();
        export.forEach((t, n) -> line("%-16s%d", t, n));
        line("%-16s%d (found by pseudonym)", "analytics_tx", subject.analyticsRows(pseudonyms));
        long analytics = subject.analyticsRows(pseudonyms);
        List<String> ids = subject.erase(pseudonyms);
        section("THE ERASURE");
        line("%-16s%d personal columns blanked", "customers", Purposes.PERSONAL_COLUMNS.size());
        line("%-16s%d rows deleted", "support_notes", export.get("support_notes"));
        line("%-16s%d rows deleted", "analytics_tx", analytics);
        line("%-16s%d rows kept: the law requires them", "transactions", export.get("transactions"));
        section("WHAT STILL MENTIONS THE CUSTOMER (every text column)");
        line("looked for: name, email, phone, national id");
        Map<String, Long> remnants = Subject.remnants(pg, ids);
        for (String t : new TreeSet<>(remnants.keySet())) {
            line("%-16s%d rows", t, remnants.get(t));
        }
        long found = remnants.values().stream().mapToLong(Long::longValue).sum();
        Subject.scrub(pg, ids);
        long after = Subject.remnants(pg, ids).values().stream().mapToLong(Long::longValue).sum();
        section("AFTER THE SCRUB");
        line("rows with a mention: %d before, %d after", found, after);
        section("THE LESSONS");
        line("no name is not anonymous: the other columns find the person");
        line("copies sit in notes, analytics and other people's records");
        line("the law can require keeping what a customer asks to erase");
    }
}
