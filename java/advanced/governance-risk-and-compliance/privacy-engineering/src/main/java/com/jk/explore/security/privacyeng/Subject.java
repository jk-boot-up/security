package com.jk.explore.security.privacyeng;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A customer's request to see their data or to have it erased: the data map, the export, the erasure and the search for what remains. */
public final class Subject {

    private final Pg pg;
    private final int id;

    public Subject(Pg pg, int id) {
        this.pg = pg;
        this.id = id;
    }

    /** The tables that hold personal data, found from the schema rather than from a list somebody keeps: the tables that point at customers, and those they point at in turn. */
    public static List<String> dataMap(Pg pg) throws SQLException {
        List<String> tables = new ArrayList<>(List.of("customers"));
        for (int round = 0; round < 3; round++) {
            for (String t : pg.strings("SELECT DISTINCT tc.table_name FROM information_schema.table_constraints tc JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name WHERE tc.constraint_type = 'FOREIGN KEY' AND ccu.table_name = ANY (?::text[]) ORDER BY 1",
                    "{" + String.join(",", tables) + "}")) {
                if (!tables.contains(t)) {
                    tables.add(t);
                }
            }
        }
        return tables;
    }

    /** How many records about the customer each table holds. */
    public Map<String, Long> export() throws SQLException {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("customers", pg.count("SELECT count(*) FROM customers WHERE id = ?", id));
        m.put("accounts", pg.count("SELECT count(*) FROM accounts WHERE customer_id = ?", id));
        m.put("transactions", pg.count("SELECT count(*) FROM transactions t JOIN accounts a ON a.id = t.account_id WHERE a.customer_id = ?", id));
        m.put("support_notes", pg.count("SELECT count(*) FROM support_notes WHERE customer_id = ?", id));
        return m;
    }

    public long analyticsRows(Pseudonyms pseudonyms) throws SQLException {
        return pg.count("SELECT count(*) FROM analytics_tx WHERE pseudonym = ?", pseudonyms.of(id));
    }

    public String name() {
        return Seed.nameOf(id);
    }

    private List<String> identifiers() throws SQLException {
        List<String> ids = new ArrayList<>(List.of(Seed.nameOf(id), Seed.emailOf(id)));
        ids.add(pg.strings("SELECT phone FROM customers WHERE id = ?", id).get(0));
        ids.add(pg.strings("SELECT national_id FROM customers WHERE id = ?", id).get(0));
        return ids;
    }

    /** Erases the customer: personal data goes, transactions stay because the law says so. Returns the identifiers to look for afterwards. */
    public List<String> erase(Pseudonyms pseudonyms) throws SQLException {
        List<String> ids = identifiers();
        pg.update("DELETE FROM support_notes WHERE customer_id = ?", id);
        pg.update("DELETE FROM analytics_tx WHERE pseudonym = ?", pseudonyms.of(id));
        pg.update("UPDATE customers SET name = 'erased', email = NULL, phone = NULL, postcode = NULL, birth_date = NULL, gender = NULL, national_id = NULL, marketing_consent = FALSE, consent_date = NULL WHERE id = ?", id);
        return ids;
    }

    /** Looks in every text column of every table for the identifiers: how many rows still mention them, by table. */
    public static Map<String, Long> remnants(Pg pg, List<String> identifiers) throws SQLException {
        Map<String, Long> found = new LinkedHashMap<>();
        for (String col : pg.strings("SELECT table_name || '.' || column_name FROM information_schema.columns WHERE table_schema = 'public' AND data_type IN ('text', 'character varying') ORDER BY 1")) {
            String table = col.substring(0, col.indexOf('.'));
            String column = col.substring(col.indexOf('.') + 1);
            StringBuilder where = new StringBuilder();
            for (int i = 0; i < identifiers.size(); i++) {
                where.append(i == 0 ? "" : " OR ").append("\"").append(column).append("\" LIKE '%' || ? || '%'");
            }
            long n = pg.count("SELECT count(*) FROM \"" + table + "\" WHERE " + where, identifiers.toArray());
            if (n > 0) {
                found.merge(table, n, Long::sum);
            }
        }
        return found;
    }

    /** Replaces the identifiers in free text and in names of counterparties with a marker. */
    public static int scrub(Pg pg, List<String> identifiers) throws SQLException {
        int changed = 0;
        for (String ident : identifiers) {
            changed += pg.update("UPDATE support_notes SET note = replace(note, ?, '[erased]') WHERE note LIKE '%' || ? || '%'", ident, ident);
            changed += pg.update("UPDATE transactions SET counterparty_name = '[erased]' WHERE counterparty_name = ?", ident);
        }
        return changed;
    }
}
