package com.jk.explore.security.provenance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON writer and reader. Objects are {@link LinkedHashMap}s, so a document is always written with its keys in the order they
 * were added, and the same document always gives the same bytes. Numbers are written and read as {@link Long}; nothing here needs fractions.
 */
final class Json {

    private Json() {
    }

    /** An object with its members in the order given: key, value, key, value... */
    static Map<String, Object> object(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            string(text, out);
        } else if (value instanceof Boolean || value instanceof Long || value instanceof Integer) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                string((String) entry.getKey(), out);
                out.append(':');
                write(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                write(list.get(i), out);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("not a JSON value: " + value.getClass().getSimpleName());
        }
    }

    private static void string(String text, StringBuilder out) {
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
                    if (c < 0x20) {
                        out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    /** Thrown for any text that is not one well-formed JSON value. */
    static final class BadJsonException extends Exception {
        BadJsonException(String message) {
            super(message);
        }
    }

    static Object read(String text) throws BadJsonException {
        Reader reader = new Reader(text);
        Object value = reader.value();
        reader.space();
        if (reader.at < text.length()) {
            throw new BadJsonException("text after the JSON value");
        }
        return value;
    }

    private static final class Reader {
        private final String text;
        private int at;

        Reader(String text) {
            this.text = text;
        }

        void space() {
            while (at < text.length() && " \t\r\n".indexOf(text.charAt(at)) >= 0) {
                at++;
            }
        }

        char peek() throws BadJsonException {
            if (at >= text.length()) {
                throw new BadJsonException("unexpected end of JSON");
            }
            return text.charAt(at);
        }

        void expect(char c) throws BadJsonException {
            if (peek() != c) {
                throw new BadJsonException("expected '" + c + "' at " + at);
            }
            at++;
        }

        Object value() throws BadJsonException {
            space();
            char c = peek();
            if (c == '{') {
                return objectValue();
            }
            if (c == '[') {
                return arrayValue();
            }
            if (c == '"') {
                return stringValue();
            }
            if (text.startsWith("true", at)) {
                at += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", at)) {
                at += 5;
                return Boolean.FALSE;
            }
            if (text.startsWith("null", at)) {
                at += 4;
                return null;
            }
            int start = at;
            if (peek() == '-') {
                at++;
            }
            while (at < text.length() && Character.isDigit(text.charAt(at))) {
                at++;
            }
            if (start == at || (at == start + 1 && text.charAt(start) == '-')) {
                throw new BadJsonException("unexpected character at " + start);
            }
            try {
                return Long.parseLong(text.substring(start, at));
            } catch (NumberFormatException e) {
                throw new BadJsonException("number out of range at " + start);
            }
        }

        Map<String, Object> objectValue() throws BadJsonException {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            space();
            if (peek() == '}') {
                at++;
                return map;
            }
            while (true) {
                space();
                String key = stringValue();
                if (map.containsKey(key)) {
                    throw new BadJsonException("duplicate member \"" + key + "\"");
                }
                space();
                expect(':');
                map.put(key, value());
                space();
                if (peek() == ',') {
                    at++;
                } else {
                    expect('}');
                    return map;
                }
            }
        }

        List<Object> arrayValue() throws BadJsonException {
            expect('[');
            List<Object> list = new ArrayList<>();
            space();
            if (peek() == ']') {
                at++;
                return list;
            }
            while (true) {
                list.add(value());
                space();
                if (peek() == ',') {
                    at++;
                } else {
                    expect(']');
                    return list;
                }
            }
        }

        String stringValue() throws BadJsonException {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                char c = peek();
                at++;
                if (c == '"') {
                    return out.toString();
                }
                if (c < 0x20) {
                    throw new BadJsonException("control character in a string");
                }
                if (c == '\\') {
                    char e = peek();
                    at++;
                    switch (e) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'u' -> {
                            if (at + 4 > text.length()) {
                                throw new BadJsonException("short unicode escape");
                            }
                            try {
                                out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                            } catch (NumberFormatException ex) {
                                throw new BadJsonException("bad unicode escape");
                            }
                            at += 4;
                        }
                        default -> throw new BadJsonException("bad escape");
                    }
                } else {
                    out.append(c);
                }
            }
        }
    }

    /** The member at a path of keys, or null if any step is missing or not an object. */
    static Object at(Object root, String... path) {
        Object current = root;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }

    static String text(Object root, String... path) {
        return at(root, path) instanceof String s ? s : null;
    }
}
