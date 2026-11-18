package com.jk.explore.security.lokilogs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A teaching class: Loki keeps one stream for each distinct set of labels, so the number of streams can be worked out before a
 * single line is sent. The demo prints this count next to the one Loki reports.
 */
final class Streams {

    private Streams() {
    }

    /** How many streams these pushes make: one per distinct label set. */
    static int count(List<Map<String, String>> labelSets) {
        Set<Map<String, String>> distinct = new HashSet<>(labelSets);
        return distinct.size();
    }

    /** A long line cut after commas into pieces of at most {@code width} characters, for printing. */
    static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder piece = new StringBuilder();
        for (String part : text.split("(?<=,)")) {
            if (piece.length() > 0 && piece.length() + part.length() > width) {
                out.add(piece.toString());
                piece.setLength(0);
            }
            piece.append(part);
        }
        out.add(piece.toString());
        return out;
    }
}
