package com.jk.explore.security.sqlihibernate;

import java.util.List;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.JDBCException;

/** Runs the product search story against a real PostgreSQL in four numbered acts and prints exact, repeatable results. */
public class SqlHibernateDemo {

    static final String JPQL_BYPASS = "zzz' or p.listed = false or p.name like 'zzz";
    static final String JPQL_ORACLE_TRUE = "zzz' or (select count(a) from Account a where a.passwordHash like 'pbkdf2$ana%') > 0 or p.name like 'zzz";
    static final String JPQL_ORACLE_FALSE = "zzz' or (select count(a) from Account a where a.passwordHash like 'pbkdf2$zed%') > 0 or p.name like 'zzz";
    static final String UNION = "zzz' union select email || ' ' || password_hash from users --";
    static final String SORT_TRICK = "price; drop table products";

    public static void main(String[] args) {
        System.out.print(run());
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
        try (SessionFactory admin = Shop.admin(lab); SessionFactory searchUser = Shop.searchUser(lab)) {
            out.append("== 1. The search page works for honest customers ==\n");
            UnsafeSearch unsafe = new UnsafeSearch(admin);
            out.append("jpql search 'Kettle' -> ").append(names(unsafe.jpql("Kettle", "p.price"))).append('\n');

            out.append("\n== 2. Hibernate does not stop a quote ==\n");
            out.append("jpql search \"").append(JPQL_BYPASS).append("\" -> ").append(names(unsafe.jpql(JPQL_BYPASS, "p.name"))).append('\n');
            out.append("jpql guess that a hash starts pbkdf2$ana -> ").append(names(unsafe.jpql(JPQL_ORACLE_TRUE, "p.name"))).append('\n');
            out.append("jpql guess that a hash starts pbkdf2$zed -> ").append(names(unsafe.jpql(JPQL_ORACLE_FALSE, "p.name"))).append('\n');
            out.append("native search \"").append(UNION).append("\" -> ").append(names(unsafe.nativeSql(UNION))).append('\n');

            out.append("\n== 3. Parameters keep text as text, in JPQL, native SQL and Criteria ==\n");
            SafeSearch safe = new SafeSearch(admin);
            out.append("jpql search 'Kettle' -> ").append(names(safe.jpql("Kettle", "price"))).append('\n');
            out.append("jpql search \"").append(JPQL_BYPASS).append("\" -> ").append(names(safe.jpql(JPQL_BYPASS, "name"))).append('\n');
            out.append("jpql guess that a hash starts pbkdf2$ana -> ").append(names(safe.jpql(JPQL_ORACLE_TRUE, "name"))).append('\n');
            out.append("native search \"").append(UNION).append("\" -> ").append(names(safe.nativeSql(UNION))).append('\n');
            out.append("criteria search \"").append(JPQL_BYPASS).append("\" -> ").append(names(safe.criteria(JPQL_BYPASS, "name"))).append('\n');
            out.append("sort \"").append(SORT_TRICK).append("\" -> ").append(names(safe.jpql("Kettle", SORT_TRICK))).append('\n');

            out.append("\n== 4. A database user with few rights limits the damage of a mistake ==\n");
            UnsafeSearch careless = new UnsafeSearch(searchUser);
            out.append("native search \"").append(UNION).append("\" as shop_search -> ").append(attempt(() -> careless.nativeSql(UNION))).append('\n');
            out.append("jpql guess that a hash starts pbkdf2$ana as shop_search -> ").append(attempt(() -> careless.jpql(JPQL_ORACLE_TRUE, "p.name"))).append('\n');
        }
        return out.toString();
    }

    private static String names(List<String> rows) {
        // A UNION has no order of its own, so rows are sorted here and every run prints the same line.
        return rows.isEmpty() ? "[no results]" : "[" + String.join(", ", rows.stream().sorted().toList()) + "]";
    }

    private static String attempt(Supplier<List<String>> search) {
        try {
            return names(search.get());
        } catch (JDBCException e) {
            String message = e.getCause().getMessage();
            return "refused: " + message.lines().findFirst().orElse(message).replace("ERROR: ", "");
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            return "refused: " + root.getMessage().lines().findFirst().orElse("").replace("ERROR: ", "");
        }
    }
}
