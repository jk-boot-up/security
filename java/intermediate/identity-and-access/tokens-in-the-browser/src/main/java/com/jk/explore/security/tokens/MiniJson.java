package com.jk.explore.security.tokens;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reads the small subset of JSON a DPoP proof uses: objects, strings and whole numbers. Anything else is an error. */
final class MiniJson {

    private final String s;
    private int i;

    private MiniJson(String s) {
        this.s = s;
    }

    static Map<String, Object> parseObject(String text) {
        MiniJson p = new MiniJson(text);
        Map<String, Object> o = p.object();
        p.ws();
        if (p.i != text.length()) {
            throw new IllegalArgumentException("trailing text");
        }
        return o;
    }

    private Map<String, Object> object() {
        ws();
        expect('{');
        Map<String, Object> m = new LinkedHashMap<>();
        ws();
        if (peek() == '}') {
            i++;
            return m;
        }
        while (true) {
            ws();
            String key = string();
            ws();
            expect(':');
            Object v = value();
            if (m.put(key, v) != null) {
                throw new IllegalArgumentException("duplicate key " + key);
            }
            ws();
            if (peek() == ',') {
                i++;
                continue;
            }
            expect('}');
            return m;
        }
    }

    private Object value() {
        ws();
        char c = peek();
        if (c == '{') {
            return object();
        }
        if (c == '"') {
            return string();
        }
        int start = i;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '-')) {
            i++;
        }
        if (start == i) {
            throw new IllegalArgumentException("unsupported value");
        }
        return Long.parseLong(s.substring(start, i));
    }

    private String string() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (peek() != '"') {
            char c = s.charAt(i++);
            if (c == '\\') {
                char n = s.charAt(i++);
                b.append(switch (n) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case '"', '\\', '/' -> n;
                    default -> throw new IllegalArgumentException("unsupported escape");
                });
            } else {
                b.append(c);
            }
        }
        i++;
        return b.toString();
    }

    private char peek() {
        if (i >= s.length()) {
            throw new IllegalArgumentException("unexpected end");
        }
        return s.charAt(i);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw new IllegalArgumentException("expected " + c);
        }
        i++;
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
    }
}
