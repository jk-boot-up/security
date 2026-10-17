package com.jk.explore.security.patchingtrivy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small JSON reader for objects, arrays, strings, numbers, booleans and null: enough for the demo's inventory and advisory files. */
final class Json {

    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        Json json = new Json(text);
        json.space();
        Object value = json.value();
        json.space();
        if (json.pos != text.length()) {
            throw json.fail("extra text after the value");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("expected a JSON object");
    }

    @SuppressWarnings("unchecked")
    static List<Object> array(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw new IllegalArgumentException("expected a JSON array");
    }

    static String string(Object value) {
        if (value instanceof String text) {
            return text;
        }
        throw new IllegalArgumentException("expected a JSON string");
    }

    private IllegalArgumentException fail(String message) {
        return new IllegalArgumentException("bad JSON at position " + pos + ": " + message);
    }

    private void space() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= text.length()) {
            throw fail("unexpected end");
        }
        return text.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw fail("expected '" + c + "'");
        }
        pos++;
    }

    private Object value() {
        char c = peek();
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> readNumber();
        };
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, pos)) {
            throw fail("unknown word");
        }
        pos += word.length();
        return value;
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        space();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            space();
            String key = readString();
            space();
            expect(':');
            space();
            if (map.put(key, value()) != null) {
                throw fail("duplicate key " + key);
            }
            space();
            if (peek() == ',') {
                pos++;
            } else {
                expect('}');
                return map;
            }
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        space();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            space();
            list.add(value());
            space();
            if (peek() == ',') {
                pos++;
            } else {
                expect(']');
                return list;
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = peek();
            pos++;
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char escape = peek();
            pos++;
            switch (escape) {
                case '"', '\\', '/' -> out.append(escape);
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    if (pos + 4 > text.length()) {
                        throw fail("short unicode escape");
                    }
                    out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> throw fail("unknown escape");
            }
        }
    }

    private Object readNumber() {
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        if (start == pos) {
            throw fail("unexpected character");
        }
        String token = text.substring(start, pos);
        if (token.indexOf('.') < 0 && token.indexOf('e') < 0 && token.indexOf('E') < 0) {
            return Long.parseLong(token);
        }
        return Double.parseDouble(token);
    }
}
