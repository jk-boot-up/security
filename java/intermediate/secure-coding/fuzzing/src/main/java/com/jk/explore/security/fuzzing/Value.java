package com.jk.explore.security.fuzzing;

import java.util.List;
import java.util.Map;

/** A value in the store's small cart format: a number, a string, a list or an object, as in JSON. */
sealed interface Value {

    record Num(long value) implements Value {
    }

    record Str(String text) implements Value {
    }

    record Items(List<Value> values) implements Value {
    }

    record Obj(Map<String, Value> fields) implements Value {
    }

    /** Writes the value back in the cart format, so that parsing the text again gives an equal value. */
    static String print(Value value) {
        return switch (value) {
            case Num n -> Long.toString(n.value());
            case Str s -> quote(s.text());
            case Items l -> {
                StringBuilder out = new StringBuilder("[");
                for (int i = 0; i < l.values().size(); i++) {
                    out.append(i == 0 ? "" : ",").append(print(l.values().get(i)));
                }
                yield out.append(']').toString();
            }
            case Obj o -> {
                StringBuilder out = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<String, Value> e : o.fields().entrySet()) {
                    out.append(first ? "" : ",").append(quote(e.getKey())).append(':').append(print(e.getValue()));
                    first = false;
                }
                yield out.append('}').toString();
            }
        };
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
