package com.jk.explore.security.fuzzing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The store's cart format, a small subset of JSON: numbers, strings with {@code \"} and {@code \\} escapes, lists and objects.
 * A {@code "note"} field that starts with {@code #} is a gift message, {@code #to:message}. The careless version has two ordinary bugs:
 * it recurses without a limit, and it reads the gift message's recipient without checking that the colon is there.
 */
final class CartParser {

    static final int MAX_DEPTH = 32;

    private static final String NOTE = "note";

    private final String text;
    private final Coverage coverage;
    private final boolean careful;
    private int at;

    private CartParser(String text, Coverage coverage, boolean careful) {
        this.text = text;
        this.coverage = coverage;
        this.careful = careful;
    }

    static Value parseCareless(String text, Coverage coverage) throws ParseException {
        return new CartParser(text, coverage, false).document();
    }

    static Value parse(String text, Coverage coverage) throws ParseException {
        return new CartParser(text, coverage, true).document();
    }

    private Value document() throws ParseException {
        Value value = value(0);
        spaces();
        if (at != text.length()) {
            throw new ParseException("text after the cart");
        }
        return value;
    }

    private int peek() {
        return at < text.length() ? text.charAt(at) : -1;
    }

    private void spaces() {
        while (peek() == ' ') {
            at++;
        }
    }

    private void expect(char c) throws ParseException {
        if (peek() != c) {
            throw new ParseException("expected " + c);
        }
        at++;
    }

    private Value value(int depth) throws ParseException {
        if (careful && depth > MAX_DEPTH) {
            throw new ParseException("nested deeper than " + MAX_DEPTH);
        }
        spaces();
        int c = peek();
        if (c == '[') {
            coverage.hit(20);
            return items(depth);
        }
        if (c == '{') {
            coverage.hit(21);
            return object(depth);
        }
        if (c == '"') {
            coverage.hit(22);
            return new Value.Str(string());
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            coverage.hit(23);
            return number();
        }
        throw new ParseException("not a value");
    }

    private Value items(int depth) throws ParseException {
        expect('[');
        List<Value> values = new ArrayList<>();
        spaces();
        if (peek() == ']') {
            at++;
            coverage.hit(30);
            return new Value.Items(values);
        }
        while (true) {
            values.add(value(depth + 1));
            spaces();
            if (peek() == ',') {
                at++;
                coverage.hit(31);
                continue;
            }
            expect(']');
            coverage.hit(32);
            return new Value.Items(values);
        }
    }

    private Value object(int depth) throws ParseException {
        expect('{');
        Map<String, Value> fields = new LinkedHashMap<>();
        spaces();
        if (peek() == '}') {
            at++;
            coverage.hit(40);
            return new Value.Obj(fields);
        }
        while (true) {
            spaces();
            if (peek() != '"') {
                throw new ParseException("a field name is a string");
            }
            String key = string();
            spaces();
            expect(':');
            Value value = value(depth + 1);
            if (isNote(key) && value instanceof Value.Str note) {
                giftMessage(note.text());
            }
            fields.put(key, value);
            spaces();
            if (peek() == ',') {
                at++;
                coverage.hit(41);
                continue;
            }
            expect('}');
            coverage.hit(42);
            return new Value.Obj(fields);
        }
    }

    /** Compares the key with "note" one letter at a time, as compiled code does; each letter matched is a branch of its own. */
    private boolean isNote(String key) {
        for (int i = 0; i < NOTE.length(); i++) {
            if (i >= key.length() || key.charAt(i) != NOTE.charAt(i)) {
                return false;
            }
            coverage.hit(50 + i);
        }
        return key.length() == NOTE.length();
    }

    private void giftMessage(String note) throws ParseException {
        coverage.hit(60);
        if (!note.isEmpty() && note.charAt(0) == '#') {
            coverage.hit(61);
            int colon = note.indexOf(':');
            if (careful && colon < 0) {
                throw new ParseException("a gift message is #to:message");
            }
            String to = note.substring(1, colon);
            if (to.isEmpty() && careful) {
                throw new ParseException("a gift message names its recipient");
            }
            coverage.hit(62);
        }
    }

    private String string() throws ParseException {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            int c = peek();
            if (c < 0) {
                throw new ParseException("a string is not closed");
            }
            at++;
            if (c == '"') {
                coverage.hit(70);
                return out.toString();
            }
            if (c == '\\') {
                int next = peek();
                if (next != '"' && next != '\\') {
                    throw new ParseException("unknown escape");
                }
                coverage.hit(71);
                at++;
                out.append((char) next);
                continue;
            }
            out.append((char) c);
        }
    }

    private Value number() throws ParseException {
        int start = at;
        if (peek() == '-') {
            at++;
        }
        int digits = 0;
        while (peek() >= '0' && peek() <= '9') {
            at++;
            digits++;
        }
        if (digits == 0 || digits > 18) {
            throw new ParseException("a number has 1 to 18 digits");
        }
        coverage.hit(80);
        return new Value.Num(Long.parseLong(text.substring(start, at)));
    }
}
