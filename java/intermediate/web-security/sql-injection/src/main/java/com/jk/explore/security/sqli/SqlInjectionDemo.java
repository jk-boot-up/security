package com.jk.explore.security.sqli;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs the product search story in four numbered acts and prints exact, repeatable results. Each attack prints the input, the
 * statement exactly as it was built, and the result, in separate labelled sections.
 */
public class SqlInjectionDemo {

    static final String BYPASS = "zzz' OR listed = 0 --";
    static final String UNION = "zzz' UNION SELECT email, password_hash FROM users --";
    static final String SORT_TRICK = "price; DROP TABLE products";

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        MiniDb db = Shop.database();
        UnsafeSearch unsafe = new UnsafeSearch(Shop.adminConnection(db));

        line(out, "ONE. The search page works for honest customers.");
        line(out, "  THE TABLE products (name, price in cents, listed):");
        for (MiniDb.Row r : Shop.adminConnection(db).query("SELECT name, price, listed FROM products")) {
            line(out, String.format("    %-19s%5s  %s%s", r.get("name"), r.get("price"), r.get("listed"),
                    Long.valueOf(0L).equals(r.get("listed")) ? "  <- NOT FOR SALE" : ""));
        }
        attack(out, "kettle", "price", UnsafeSearch.statement("kettle", "price"), names(unsafe.search("kettle", "price")), "");

        line(out, "TWO. A quote turns the customer's text into SQL.");
        attack(out, BYPASS, "name", UnsafeSearch.statement(BYPASS, "name"), names(unsafe.search(BYPASS, "name")), "AN UNLISTED PRODUCT");
        attack(out, UNION, "name", UnsafeSearch.statement(UNION, "name"), names(unsafe.search(UNION, "name")), "CUSTOMERS' E-MAILS");
        attack(out, "kettle", SORT_TRICK, UnsafeSearch.statement("kettle", SORT_TRICK), attempt(() -> unsafe.search("kettle", SORT_TRICK)), "");
        line(out, "  stopped by luck: this engine runs one statement per call");

        line(out, "THREE. Parameters keep text as text.");
        SafeSearch safe = new SafeSearch(Shop.adminConnection(db));
        line(out, "  THE STATEMENT (fixed text; the words travel separately):");
        sql(out, SafeSearch.statement("name").replace("ORDER BY name", "ORDER BY <name or price, from a list>"));
        line(out, "  THE INPUTS AND RESULTS (each input is the value of ?):");
        for (String term : List.of("kettle", BYPASS, UNION)) {
            line(out, "    " + term);
            line(out, "      -> " + names(safe.search(term, "name")));
        }
        line(out, "  THE SORT BOX (a column name cannot be a ?):");
        line(out, "    " + SORT_TRICK + " is not on the list");
        line(out, "      -> ORDER BY " + SortColumn.parse(SORT_TRICK).column() + ", the default");
        line(out, "      -> " + names(safe.search("kettle", SORT_TRICK)));

        line(out, "FOUR. A database user with few rights limits the damage of a mistake.");
        line(out, "  THE TWO DATABASE USERS (the tables each may read):");
        line(out, "    admin   " + String.join(", ", Shop.ADMIN_TABLES));
        line(out, "    search  " + String.join(", ", Shop.SEARCH_TABLES));
        UnsafeSearch careless = new UnsafeSearch(Shop.searchConnection(db));
        line(out, "  THE UNION ATTACK, STILL PASTED IN:");
        line(out, "    as admin   " + names(unsafe.search(UNION, "name")));
        line(out, "    as search  " + attempt(() -> careless.search(UNION, "name")));
        line(out, "  THE QUOTE ATTACK, AS search:");
        line(out, "    " + attempt(() -> careless.search(BYPASS, "name")) + "  <- STILL WORKS");
        line(out, "  few rights limit the damage; only parameters fix the flaw");
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The input, the statement as built, and the result, each in its own section. */
    private static void attack(StringBuilder out, String term, String sort, String statement, String result, String mark) {
        line(out, "  THE INPUT (search box, sort box):");
        line(out, "    " + term + "    " + sort);
        line(out, "  THE STATEMENT AS BUILT (the input pasted in):");
        sql(out, statement);
        line(out, "  THE RESULT:");
        line(out, "    " + result + (mark.isEmpty() ? "" : "  <- " + mark));
    }

    /**
     * One statement over several lines: a new line before WHERE, UNION and ORDER BY. A comment (--) runs to the end of the
     * statement, so everything after it stays on one last line, ORDER BY included.
     */
    private static void sql(StringBuilder out, String statement) {
        int comment = statement.indexOf(" --");
        String code = comment < 0 ? statement : statement.substring(0, comment);
        String text = code.replace(" WHERE ", "\nWHERE ").replace(" UNION ", "\nUNION ").replace(" ORDER BY ", "\nORDER BY ");
        for (String part : text.split("\n")) {
            line(out, "    " + part);
        }
        if (comment >= 0) {
            line(out, "    " + statement.substring(comment + 1));
        }
    }

    private static String names(List<MiniDb.Row> rows) {
        return rows.isEmpty() ? "[no results]"
                : rows.stream().map(r -> String.valueOf(r.get("name"))).collect(Collectors.joining(", ", "[", "]"));
    }

    private static String attempt(java.util.function.Supplier<List<MiniDb.Row>> search) {
        try {
            return names(search.get());
        } catch (SqlException e) {
            return "refused: " + e.getMessage();
        }
    }
}
