package com.jk.explore.security.bff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, strict JSON reader and writer for token headers and claims: objects, arrays, strings, integers, booleans and null.
 * Strict on purpose. A duplicate member name is refused (RFC 7515 section 4 and RFC 8725 section 3.12), because two parsers that
 * keep different copies of a duplicated "alg" or "sub" read the same token differently.
 */
final class Json {

    private final String text;
    private int at;

    private Json(String text) {
        this.text = text;
    }

    static Map<String, Object> parseObject(String text) throws InvalidTokenException {
        Json json = new Json(text);
        json.space();
        if (json.peek() != '{') {
            throw new InvalidTokenException("not a JSON object");
        }
        Map<String, Object> value = json.object();
        json.space();
        if (json.at != text.length()) {
            throw new InvalidTokenException("text after the JSON object");
        }
        return value;
    }

    private char peek() throws InvalidTokenException {
        if (at >= text.length()) {
            throw new InvalidTokenException("JSON ends too early");
        }
        return text.charAt(at);
    }

    private void expect(char c) throws InvalidTokenException {
        if (peek() != c) {
            throw new InvalidTokenException("malformed JSON");
        }
        at++;
    }

    private void space() {
        while (at < text.length() && " \t\r\n".indexOf(text.charAt(at)) >= 0) {
            at++;
        }
    }

    private Object value() throws InvalidTokenException {
        space();
        char c = peek();
        if (c == '{') {
            return object();
        }
        if (c == '[') {
            return array();
        }
        if (c == '"') {
            return string();
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            return number();
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
        throw new InvalidTokenException("malformed JSON");
    }

    private Map<String, Object> object() throws InvalidTokenException {
        expect('{');
        Map<String, Object> members = new LinkedHashMap<>();
        space();
        if (peek() == '}') {
            at++;
            return members;
        }
        while (true) {
            space();
            String name = string();
            if (members.containsKey(name)) {
                throw new InvalidTokenException("duplicate member \"" + name + "\"");
            }
            space();
            expect(':');
            members.put(name, value());
            space();
            if (peek() == ',') {
                at++;
                continue;
            }
            expect('}');
            return members;
        }
    }

    private List<Object> array() throws InvalidTokenException {
        expect('[');
        List<Object> items = new ArrayList<>();
        space();
        if (peek() == ']') {
            at++;
            return items;
        }
        while (true) {
            items.add(value());
            space();
            if (peek() == ',') {
                at++;
                continue;
            }
            expect(']');
            return items;
        }
    }

    private String string() throws InvalidTokenException {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = peek();
            at++;
            if (c == '"') {
                return out.toString();
            }
            if (c < 0x20) {
                throw new InvalidTokenException("a control character in a JSON string");
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char e = peek();
            at++;
            switch (e) {
                case '"', '\\', '/' -> out.append(e);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (at + 4 > text.length()) {
                        throw new InvalidTokenException("malformed JSON");
                    }
                    try {
                        out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                    } catch (NumberFormatException ex) {
                        throw new InvalidTokenException("malformed JSON");
                    }
                    at += 4;
                }
                default -> throw new InvalidTokenException("malformed JSON");
            }
        }
    }

    private Long number() throws InvalidTokenException {
        int start = at;
        if (peek() == '-') {
            at++;
        }
        while (at < text.length() && Character.isDigit(text.charAt(at))) {
            at++;
        }
        if (at < text.length() && ".eE".indexOf(text.charAt(at)) >= 0) {
            throw new InvalidTokenException("only whole numbers are used in these tokens");
        }
        try {
            return Long.parseLong(text.substring(start, at));
        } catch (NumberFormatException e) {
            throw new InvalidTokenException("malformed JSON");
        }
    }

    /** Writes a map as compact JSON, in its own order. */
    static String write(Map<String, ?> members) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> member : members.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(out, member.getKey());
            out.append(':');
            writeValue(out, member.getValue());
        }
        return out.append('}').toString();
    }

    private static void writeValue(StringBuilder out, Object value) {
        if (value instanceof String s) {
            writeString(out, s);
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                writeValue(out, list.get(i));
            }
            out.append(']');
        } else if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), v));
            out.append(write(copy));
        } else {
            out.append(value);
        }
    }

    private static void writeString(StringBuilder out, String s) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
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
}
