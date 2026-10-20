package com.jk.explore.security.sqli;

import java.util.List;
import java.util.stream.Collectors;

/** Runs the product search story in four numbered acts and prints exact, repeatable results. */
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

        out.append("== 1. The search page works for honest customers ==\n");
        UnsafeSearch unsafe = new UnsafeSearch(Shop.adminConnection(db));
        out.append("search 'kettle' -> ").append(names(unsafe.search("kettle", "price"))).append('\n');

        out.append("\n== 2. A quote turns the customer's text into SQL ==\n");
        out.append("search \"").append(BYPASS).append("\" -> ").append(names(unsafe.search(BYPASS, "name"))).append('\n');
        out.append("search \"").append(UNION).append("\" -> ").append(names(unsafe.search(UNION, "name"))).append('\n');
        out.append("sort \"").append(SORT_TRICK).append("\" -> ").append(attempt(() -> unsafe.search("kettle", SORT_TRICK))).append('\n');

        out.append("\n== 3. Parameters keep text as text ==\n");
        SafeSearch safe = new SafeSearch(Shop.adminConnection(db));
        out.append("search 'kettle' -> ").append(names(safe.search("kettle", "price"))).append('\n');
        out.append("search \"").append(BYPASS).append("\" -> ").append(names(safe.search(BYPASS, "name"))).append('\n');
        out.append("search \"").append(UNION).append("\" -> ").append(names(safe.search(UNION, "name"))).append('\n');
        out.append("sort \"").append(SORT_TRICK).append("\" -> ").append(names(safe.search("kettle", SORT_TRICK))).append('\n');

        out.append("\n== 4. A database user with few rights limits the damage of a mistake ==\n");
        UnsafeSearch careless = new UnsafeSearch(Shop.searchConnection(db));
        out.append("search \"").append(UNION).append("\" -> ").append(attempt(() -> careless.search(UNION, "name"))).append('\n');
        return out.toString();
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
