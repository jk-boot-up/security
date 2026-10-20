package com.jk.explore.security.sqli;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A tiny SQL engine, just enough to show injection with the JDK alone. It reads one statement the way a real database does: it splits the text
 * into tokens first, so whatever a customer typed into a quoted string becomes <em>code</em> the moment it closes the quote.
 * <p>Supported: {@code SELECT cols FROM t [WHERE ...] [ORDER BY c [ASC|DESC]] [UNION SELECT ...]}, {@code = <> < > LIKE AND OR ( )}, {@code ?}
 * parameters and {@code --} comments. Like most drivers, it refuses a second statement after a {@code ;}.
 */
public final class MiniDb {

    /** One row, by column name. */
    public record Row(Map<String, Object> values) {
        public Object get(String column) {
            return values.get(column);
        }
    }

    private final Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
    private final Map<String, List<String>> columns = new LinkedHashMap<>();

    public void createTable(String name, String... cols) {
        tables.put(name, new ArrayList<>());
        columns.put(name, List.of(cols));
    }

    public void insert(String table, Object... values) {
        List<String> cols = columns.get(table);
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < cols.size(); i++) {
            row.put(cols.get(i), values[i]);
        }
        tables.get(table).add(row);
    }

    /** Opens a connection that may read only the given tables, like a database user with limited grants. */
    public Connection connect(Set<String> readableTables) {
        return new Connection(readableTables);
    }

    public final class Connection {
        private final Set<String> grants;

        private Connection(Set<String> grants) {
            this.grants = grants;
        }

        public List<Row> query(String sql, Object... params) {
            Parser p = new Parser(tokenize(sql), params, grants);
            List<Row> rows = p.statement();
            p.end();
            return rows;
        }
    }

    // ---- tokens ----

    private enum Kind { WORD, STRING, NUMBER, SYMBOL, PARAM }

    private record Token(Kind kind, String text) {
        boolean is(String word) {
            return (kind == Kind.WORD || kind == Kind.SYMBOL) && text.equalsIgnoreCase(word);
        }
    }

    private static List<Token> tokenize(String sql) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (sql.startsWith("--", i)) {
                break; // the rest of the line is a comment: this is what injected "--" uses to cut off the original query's tail
            } else if (c == '\'') {
                StringBuilder s = new StringBuilder();
                i++;
                while (true) {
                    if (i >= sql.length()) {
                        throw new SqlException("unterminated string");
                    }
                    if (sql.charAt(i) == '\'') {
                        if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                            s.append('\'');
                            i += 2;
                            continue;
                        }
                        i++;
                        break;
                    }
                    s.append(sql.charAt(i++));
                }
                out.add(new Token(Kind.STRING, s.toString()));
            } else if (Character.isDigit(c)) {
                int j = i;
                while (j < sql.length() && Character.isDigit(sql.charAt(j))) {
                    j++;
                }
                out.add(new Token(Kind.NUMBER, sql.substring(i, j)));
                i = j;
            } else if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < sql.length() && (Character.isLetterOrDigit(sql.charAt(j)) || sql.charAt(j) == '_')) {
                    j++;
                }
                out.add(new Token(Kind.WORD, sql.substring(i, j)));
                i = j;
            } else if (c == '?') {
                out.add(new Token(Kind.PARAM, "?"));
                i++;
            } else if (sql.startsWith("<>", i)) {
                out.add(new Token(Kind.SYMBOL, "<>"));
                i += 2;
            } else if ("=<>(),*;".indexOf(c) >= 0) {
                out.add(new Token(Kind.SYMBOL, String.valueOf(c)));
                i++;
            } else {
                throw new SqlException("unexpected character '" + c + "'");
            }
        }
        return out;
    }

    // ---- parser and evaluator ----

    private final class Parser {
        private final List<Token> tokens;
        private final Object[] params;
        private final Set<String> grants;
        private int pos;
        private int nextParam;
        private List<String> lastNames = List.of();

        Parser(List<Token> tokens, Object[] params, Set<String> grants) {
            this.tokens = tokens;
            this.params = params;
            this.grants = grants;
        }

        void end() {
            if (pos < tokens.size()) {
                throw new SqlException("unexpected '" + tokens.get(pos).text() + "' (one statement only)");
            }
        }

        List<Row> statement() {
            List<Row> rows = select();
            List<String> names = lastNames;
            while (peekIs("UNION")) {
                pos++;
                List<Row> more = select();
                if (names.size() != lastNames.size()) {
                    throw new SqlException("UNION needs the same number of columns");
                }
                // a UNION answers in the first query's column names, whatever the second one asked for
                for (Row r : more) {
                    Map<String, Object> renamed = new LinkedHashMap<>();
                    int k = 0;
                    for (Object v : r.values().values()) {
                        renamed.put(names.get(k++), v);
                    }
                    rows.add(new Row(renamed));
                }
            }
            return rows;
        }

        private List<Row> select() {
            expect("SELECT");
            List<String> wanted = new ArrayList<>();
            if (peekIs("*")) {
                pos++;
            } else {
                do {
                    wanted.add(word());
                } while (acceptComma());
            }
            expect("FROM");
            String table = word();
            if (!tables.containsKey(table)) {
                throw new SqlException("no such table: " + table);
            }
            if (!grants.contains(table)) {
                throw new SqlException("permission denied for table " + table);
            }
            for (String c : wanted) {
                if (!columns.get(table).contains(c)) {
                    throw new SqlException("no such column: " + c);
                }
            }
            Predicate<Map<String, Object>> where = r -> true;
            if (peekIs("WHERE")) {
                pos++;
                where = or(table);
            }
            String orderBy = null;
            boolean desc = false;
            if (peekIs("ORDER")) {
                pos++;
                expect("BY");
                orderBy = word();
                if (!columns.get(table).contains(orderBy)) {
                    throw new SqlException("no such column: " + orderBy);
                }
                if (peekIs("DESC")) {
                    pos++;
                    desc = true;
                } else if (peekIs("ASC")) {
                    pos++;
                }
            }
            List<Map<String, Object>> picked = new ArrayList<>();
            for (Map<String, Object> r : tables.get(table)) {
                if (where.test(r)) {
                    picked.add(r);
                }
            }
            if (orderBy != null) {
                String col = orderBy;
                Comparator<Map<String, Object>> cmp = (a, b) -> compare(a.get(col), b.get(col));
                picked.sort(desc ? cmp.reversed() : cmp);
            }
            List<String> shown = wanted.isEmpty() ? columns.get(table) : wanted;
            lastNames = shown;
            List<Row> rows = new ArrayList<>();
            for (Map<String, Object> r : picked) {
                Map<String, Object> out = new LinkedHashMap<>();
                for (String c : shown) {
                    out.put(c, r.get(c));
                }
                rows.add(new Row(out));
            }
            return rows;
        }

        private Predicate<Map<String, Object>> or(String table) {
            Predicate<Map<String, Object>> left = and(table);
            while (peekIs("OR")) {
                pos++;
                Predicate<Map<String, Object>> l = left;
                Predicate<Map<String, Object>> right = and(table);
                left = r -> l.test(r) || right.test(r);
            }
            return left;
        }

        private Predicate<Map<String, Object>> and(String table) {
            Predicate<Map<String, Object>> left = comparison(table);
            while (peekIs("AND")) {
                pos++;
                Predicate<Map<String, Object>> l = left;
                Predicate<Map<String, Object>> right = comparison(table);
                left = r -> l.test(r) && right.test(r);
            }
            return left;
        }

        private Predicate<Map<String, Object>> comparison(String table) {
            if (peekIs("(")) {
                pos++;
                Predicate<Map<String, Object>> inner = or(table);
                expect(")");
                return inner;
            }
            Operand a = operand(table);
            String op = tokens.size() > pos ? tokens.get(pos++).text().toUpperCase() : "";
            Operand b = operand(table);
            return switch (op) {
                case "=" -> r -> compare(a.value(r), b.value(r)) == 0;
                case "<>" -> r -> compare(a.value(r), b.value(r)) != 0;
                case "<" -> r -> compare(a.value(r), b.value(r)) < 0;
                case ">" -> r -> compare(a.value(r), b.value(r)) > 0;
                case "LIKE" -> r -> like(String.valueOf(a.value(r)), String.valueOf(b.value(r)));
                default -> throw new SqlException("unknown operator '" + op + "'");
            };
        }

        private interface Operand {
            Object value(Map<String, Object> row);
        }

        private Operand operand(String table) {
            if (pos >= tokens.size()) {
                throw new SqlException("unexpected end of statement");
            }
            Token t = tokens.get(pos++);
            return switch (t.kind()) {
                case STRING -> r -> t.text();
                case NUMBER -> r -> Long.parseLong(t.text());
                case PARAM -> {
                    Object v = params[nextParam++]; // a parameter is only ever a value: it is never parsed again
                    yield r -> v;
                }
                case WORD -> {
                    if (!columns.get(table).contains(t.text())) {
                        throw new SqlException("no such column: " + t.text());
                    }
                    yield r -> r.get(t.text());
                }
                default -> throw new SqlException("unexpected '" + t.text() + "'");
            };
        }

        private boolean peekIs(String word) {
            return pos < tokens.size() && tokens.get(pos).is(word);
        }

        private boolean acceptComma() {
            if (peekIs(",")) {
                pos++;
                return true;
            }
            return false;
        }

        private void expect(String word) {
            if (!peekIs(word)) {
                String got = pos < tokens.size() ? tokens.get(pos).text() : "end of statement";
                throw new SqlException("expected " + word + " but found " + got);
            }
            pos++;
        }

        private String word() {
            if (pos >= tokens.size() || tokens.get(pos).kind() != Kind.WORD) {
                throw new SqlException("expected a name");
            }
            return tokens.get(pos++).text();
        }
    }

    private static int compare(Object a, Object b) {
        if (a instanceof Long x && b instanceof Long y) {
            return Long.compare(x, y);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private static boolean like(String value, String pattern) {
        StringBuilder rx = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            rx.append(c == '%' ? ".*" : Pattern.quote(String.valueOf(c)));
        }
        return Pattern.compile(rx.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(value).matches();
    }
}
