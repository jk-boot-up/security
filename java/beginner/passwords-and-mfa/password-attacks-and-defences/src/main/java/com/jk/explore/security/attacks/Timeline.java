package com.jk.explore.security.attacks;

import java.util.ArrayList;
import java.util.List;

/**
 * An attack's steps folded into runs of the same answer, such as "ticks 5-14  no guess  LOCKED", so that the
 * tick at which a defended attack finally wins can be worked out line by line.
 */
public final class Timeline {

    private Timeline() {
    }

    public static List<String> runs(List<Step> steps) {
        List<String> lines = new ArrayList<>();
        int guesses = 0;
        int start = 0;
        while (start < steps.size()) {
            int end = start;
            while (end + 1 < steps.size() && steps.get(end + 1).outcome() == steps.get(start).outcome()) {
                end++;
            }
            Step first = steps.get(start);
            String guessed;
            if (first.refused()) {
                guessed = "no guess";                       // a refused attempt does not use up a word
            } else {
                int from = guesses + 1;
                guesses += end - start + 1;
                guessed = from == guesses ? "guess " + from : "guesses " + from + "-" + guesses;
            }
            String ticks = start == end ? "tick " + first.tick() : "ticks " + first.tick() + "-" + steps.get(end).tick();
            lines.add(String.format("%-13s %-15s %s", ticks, guessed, first.outcome()));
            start = end + 1;
        }
        return lines;
    }
}
