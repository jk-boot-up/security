package com.jk.explore.security.sqlihibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;

/**
 * Runs the product search story against a real PostgreSQL in four numbered acts and prints exact, repeatable results. Each attack
 * prints the input, the query exactly as it was built, the SQL Hibernate sent, and the result, in separate labelled sections.
 */
public class SqlHibernateDemo {

    static final String JPQL_BYPASS = "zzz' or p.listed = false or p.name like 'zzz";
    static final String JPQL_ORACLE_TRUE = oracle("pbkdf2$ana");
    static final String JPQL_ORACLE_FALSE = oracle("pbkdf2$zed");
    static final String UNION = "zzz' union select email || ' ' || password_hash from users --";
    static final String SORT_TRICK = "price; drop table products";
    static final int WIDTH = 66;

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** A yes or no question about the account table, hidden in a product search. */
    static String oracle(String prefix) {
        return "zzz' or exists (from Account a where a.passwordHash like '" + prefix + "%') or p.name like 'zzz";
    }

    /** Starts a PostgreSQL container, runs the story and removes the container. Needs Docker. */
    public static String run() {
        try (PostgresLab lab = PostgresLab.start()) {
            Shop.load(lab);
            return story(lab);
        }
    }

    static String story(PostgresLab lab) {
        StringBuilder out = new StringBuilder();
        SqlLog sent = new SqlLog();
        try (SessionFactory admin = Shop.admin(lab, sent); SessionFactory searchUser = Shop.searchUser(lab)) {
            UnsafeSearch unsafe = new UnsafeSearch(admin);

            line(out, "ONE. The search page works for honest customers.");
            line(out, "  THE TABLE products (name, price in cents, listed):");
            for (Object[] r : admin.fromSession(s -> s.createQuery("select p.name, p.price, p.listed from Product p order by p.id", Object[].class).getResultList())) {
                line(out, String.format("    %-19s%5s  %s%s", r[0], r[1], r[2], Boolean.FALSE.equals(r[2]) ? "  <- NOT FOR SALE" : ""));
            }
            line(out, "  THE TABLE users (email, password_hash), another entity:");
            for (Object[] r : admin.fromSession(s -> s.createQuery("select a.email, a.passwordHash from Account a order by a.id", Object[].class).getResultList())) {
                line(out, String.format("    %-19s%s", r[0], r[1]));
            }
            input(out, "Kettle", "p.price");
            block(out, "  THE JPQL AS BUILT (the input pasted in):", UnsafeSearch.jpqlStatement("Kettle", "p.price"));
            String honest = names(unsafe.jpql("Kettle", "p.price"));
            block(out, "  THE SQL HIBERNATE SENT (Product became products):", sent.last());
            result(out, honest, "");

            line(out, "TWO. Hibernate does not stop a quote.");
            input(out, JPQL_BYPASS, "p.name");
            block(out, "  THE JPQL AS BUILT (the input pasted in):", UnsafeSearch.jpqlStatement(JPQL_BYPASS, "p.name"));
            result(out, names(unsafe.jpql(JPQL_BYPASS, "p.name")), "AN UNLISTED PRODUCT");
            input(out, JPQL_ORACLE_TRUE, "p.name");
            List<String> yes = unsafe.jpql(JPQL_ORACLE_TRUE, "p.name");
            block(out, "  THE SQL HIBERNATE SENT (Account became users):", sent.last());
            List<String> no = unsafe.jpql(JPQL_ORACLE_FALSE, "p.name");
            line(out, "  THE ANSWERS (a full page is yes, an empty page is no):");
            line(out, "    a hash starts pbkdf2$ana -> " + count(yes) + "  <- YES");
            line(out, "    a hash starts pbkdf2$zed -> " + count(no) + "  <- NO");
            line(out, "  one yes or no per search, letter by letter");
            line(out, "  THE INPUT (search box, sent to a native SQL query):");
            wrap(out, "    ", UNION);
            block(out, "  THE SQL AS BUILT (the input pasted in):", UnsafeSearch.nativeStatement(UNION));
            line(out, "  THE RESULT:");
            for (String row : sorted(unsafe.nativeSql(UNION))) {
                line(out, "    " + row + "  <- E-MAIL AND HASH");
            }

            line(out, "THREE. Parameters keep text as text.");
            SafeSearch safe = new SafeSearch(admin);
            block(out, "  THE JPQL (fixed text; the words travel as :term):",
                    SafeSearch.jpqlStatement("name").replace("order by p.name", "order by <p.name or p.price, from a list>"));
            safe.jpql("Kettle", "name");
            block(out, "  THE SQL HIBERNATE SENT (:term became ?):", sent.last());
            line(out, "  THE VALUE OF ? AND THE RESULT:");
            for (String term : List.of("Kettle", JPQL_BYPASS, JPQL_ORACLE_TRUE)) {
                wrap(out, "    ", "%" + term + "%");
                line(out, "      -> " + names(safe.jpql(term, "name")));
            }
            line(out, "  THE SAME PAYLOADS IN NATIVE SQL AND CRITERIA:");
            line(out, "    native SQL with the union input   -> " + names(safe.nativeSql(UNION)));
            String criteria = names(safe.criteria(JPQL_BYPASS, "name"));
            line(out, "    Criteria API with the quote input -> " + criteria);
            block(out, "  THE SQL THE CRITERIA QUERY SENT (a ? again):", sent.last());
            line(out, "  THE SORT BOX (a column name cannot be a ?):");
            line(out, "    " + SORT_TRICK + " is not on the list");
            line(out, "      -> order by " + SortColumn.parse(SORT_TRICK).jpql() + ", the default");
            line(out, "      -> " + names(safe.jpql("Kettle", SORT_TRICK)));

            line(out, "FOUR. A database user with few rights limits the damage.");
            line(out, "  THE TWO DATABASE USERS (the tables each may read):");
            line(out, "    postgres     every table (it owns them)");
            line(out, "    " + Shop.SEARCH_USER + "  " + String.join(", ", Shop.grants(admin, Shop.SEARCH_USER)));
            UnsafeSearch careless = new UnsafeSearch(searchUser);
            line(out, "  THE UNION ATTACK, STILL PASTED IN:");
            line(out, "    as postgres     " + unsafe.nativeSql(UNION).size() + " e-mails and hashes");
            line(out, "    as shop_search  " + attempt(() -> careless.nativeSql(UNION)));
            line(out, "  THE HASH QUESTION, AS shop_search:");
            line(out, "    " + attempt(() -> careless.jpql(JPQL_ORACLE_TRUE, "p.name")));
            line(out, "  THE QUOTE ATTACK, AS shop_search:");
            line(out, "    " + attempt(() -> careless.jpql(JPQL_BYPASS, "p.name")) + "  <- STILL WORKS");
            line(out, "  few rights limit the damage; only parameters fix the flaw");
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void input(StringBuilder out, String term, String sort) {
        line(out, "  THE INPUT (search box, then sort box):");
        wrap(out, "    ", term);
        line(out, "    " + sort);
    }

    private static void result(StringBuilder out, String result, String mark) {
        line(out, "  THE RESULT:");
        line(out, "    " + result + (mark.isEmpty() ? "" : "  <- " + mark));
    }

    private static void block(StringBuilder out, String header, String statement) {
        line(out, header);
        for (String part : layout(statement)) {
            line(out, "    " + part);
        }
    }

    private static void wrap(StringBuilder out, String indent, String text) {
        List<String> parts = wrap(text, WIDTH - indent.length());
        for (int i = 0; i < parts.size(); i++) {
            line(out, indent + (i == 0 ? "" : "  ") + parts.get(i));
        }
    }

    /**
     * One statement over several lines: a new line before the first where, before union and order by, and before a comment (--),
     * which runs to the end, so everything after it stays on one last line. A part that is still too long is split before and, or
     * and where, then at spaces, and its next lines are indented by two. Joining the trimmed lines with one space gives the statement
     * back.
     */
    static List<String> layout(String statement) {
        int comment = statement.indexOf(" --");
        String code = comment < 0 ? statement : statement.substring(0, comment);
        String text = code.replaceFirst(" where ", "\nwhere ").replace(" union ", "\nunion ").replace(" order by ", "\norder by ");
        List<String> lines = new ArrayList<>();
        for (String part : text.split("\n")) {
            List<String> packed = pack(part.split(" (?=(and|or|where) )"), WIDTH - 6);
            for (int i = 0; i < packed.size(); i++) {
                lines.add(i == 0 ? packed.get(i) : "  " + packed.get(i));
            }
        }
        if (comment >= 0) {
            lines.add(statement.substring(comment + 1));
        }
        return lines;
    }

    /** Greedy wrap at spaces; a word longer than the width stays whole. */
    static List<String> wrap(String text, int width) {
        return pack(text.split(" "), width);
    }

    /** Puts pieces on a line, one space apart, while they fit; a piece that does not fit alone is wrapped at its spaces. */
    private static List<String> pack(String[] pieces, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String piece : pieces) {
            if (current.isEmpty()) {
                current.append(piece);
            } else if (current.length() + 1 + piece.length() <= width) {
                current.append(' ').append(piece);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(piece);
            }
            if (current.length() > width && current.indexOf(" ") > 0) {
                List<String> words = wrap(current.toString(), width);
                lines.addAll(words.subList(0, words.size() - 1));
                current.setLength(0);
                current.append(words.getLast());
            }
        }
        lines.add(current.toString());
        return lines;
    }

    private static List<String> sorted(List<String> rows) {
        // A UNION has no order of its own, so rows are sorted here and every run prints the same lines.
        return rows.stream().sorted().toList();
    }

    private static String names(List<String> rows) {
        return rows.isEmpty() ? "[no results]" : "[" + String.join(", ", sorted(rows)) + "]";
    }

    private static String count(List<String> rows) {
        return rows.isEmpty() ? "[no results]" : rows.size() + " products";
    }

    private static String attempt(Supplier<List<String>> search) {
        try {
            return names(search.get());
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String message = root.getMessage() == null ? "" : root.getMessage();
            return "refused: " + message.lines().findFirst().orElse("").replace("ERROR: ", "");
        }
    }
}
