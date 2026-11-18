package com.jk.explore.security.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A teaching class: redoes the checks of {@link Quantity#parse} and {@link Username#parse} one step at a time, so the demo can show
 * how a value is read. The tests prove the steps end in the same result as the real classes.
 */
public final class Steps {

    /** One step: what was checked or changed, and the text after it. */
    public record Step(String name, String result) {
    }

    private Steps() {
    }

    /** The checks of Quantity.parse, in order, stopping at the first one that refuses. */
    public static List<Step> quantity(String raw) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("length", raw.length() + " of at most 3 characters: " + (raw.length() <= 3 ? "ok" : "refused")));
        if (raw.length() > 3) {
            return steps;
        }
        String bad = raw.chars().filter(c -> c < '0' || c > '9').mapToObj(c -> "'" + (char) c + "'").collect(Collectors.joining(" "));
        if (raw.isEmpty() || !bad.isEmpty()) {
            steps.add(new Step("digits", (bad.isEmpty() ? "empty" : bad + " is not a digit") + ": refused"));
            return steps;
        }
        steps.add(new Step("digits", "only 0 to 9: ok"));
        int value = Integer.parseInt(raw);
        boolean inRange = value >= 1 && value <= Quantity.MAX;
        steps.add(new Step("range", value + " is " + (inRange ? "" : "not ") + "from 1 to " + Quantity.MAX + ": " + (inRange ? "ok" : "refused")));
        if (inRange) {
            steps.add(new Step("value", new Quantity(value).toString()));
        }
        return steps;
    }

    /** The canonical form of a login name, step by step: as typed, NFKC, strip, lower-case. */
    public static List<Step> username(String raw) {
        String nfkc = Text.nfkc(raw);
        String stripped = nfkc.strip();
        String lower = stripped.toLowerCase(Locale.ROOT);
        return List.of(new Step("as typed", codePoints(raw)),
                new Step("NFKC", nfkc + "  " + codePoints(nfkc)),
                new Step("strip", stripped),
                new Step("lower-case", lower));
    }

    /** The text with every character that is not a plain ASCII letter written as [U+XXXX], so hidden ones can be seen. */
    public static String visible(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(c -> out.append(c < 0x80 && Character.isLetter(c) ? Character.toString(c)
                : String.format(Locale.ROOT, "[U+%04X]", c)));
        return out.toString();
    }

    /** The Unicode code points of a text, such as U+0061 U+006E. */
    public static String codePoints(String text) {
        return text.codePoints().mapToObj(c -> String.format(Locale.ROOT, "U+%04X", c)).collect(Collectors.joining(" "));
    }
}
