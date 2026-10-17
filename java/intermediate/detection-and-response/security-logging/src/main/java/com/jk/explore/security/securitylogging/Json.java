package com.jk.explore.security.securitylogging;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A flat JSON object of string values, written and read without a library. Writing escapes quotes, backslashes and every control
 * character, so text a user typed can never end a line, start a new record or add a field.
 */
final class Json {

    private Json() {
    }

    static String write(Map<String, String> fields) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            quote(out, field.getKey());
            out.append(':');
            quote(out, field.getValue());
        }
        return out.append('}').toString();
    }

    private static void quote(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || c == ' ' || c == ' ') {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    /** Reads one line written by {@link #write}. Anything else is refused with IllegalArgumentException. */
    static Map<String, String> parse(String line) {
        Map<String, String> fields = new LinkedHashMap<>();
        int[] at = {0};
        expect(line, at, '{');
        if (peek(line, at) == '}') {
            at[0]++;
        } else {
            while (true) {
                String key = string(line, at);
                expect(line, at, ':');
                String value = string(line, at);
                if (fields.put(key, value) != null) {
                    throw new IllegalArgumentException("a field appears twice: " + key);
                }
                char next = peek(line, at);
                at[0]++;
                if (next == '}') {
                    break;
                }
                if (next != ',') {
                    throw new IllegalArgumentException("expected , or }");
                }
            }
        }
        if (at[0] != line.length()) {
            throw new IllegalArgumentException("text after the object");
        }
        return fields;
    }

    private static char peek(String line, int[] at) {
        if (at[0] >= line.length()) {
            throw new IllegalArgumentException("the line ends too early");
        }
        return line.charAt(at[0]);
    }

    private static void expect(String line, int[] at, char wanted) {
        if (peek(line, at) != wanted) {
            throw new IllegalArgumentException("expected " + wanted);
        }
        at[0]++;
    }

    private static String string(String line, int[] at) {
        expect(line, at, '"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = peek(line, at);
            at[0]++;
            if (c == '"') {
                return out.toString();
            }
            if (c < 0x20) {
                throw new IllegalArgumentException("a raw control character inside a string");
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char e = peek(line, at);
            at[0]++;
            switch (e) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (at[0] + 4 > line.length()) {
                        throw new IllegalArgumentException("a short \\u escape");
                    }
                    out.append((char) Integer.parseInt(line.substring(at[0], at[0] + 4), 16));
                    at[0] += 4;
                }
                default -> throw new IllegalArgumentException("an unknown escape");
            }
        }
    }
}
