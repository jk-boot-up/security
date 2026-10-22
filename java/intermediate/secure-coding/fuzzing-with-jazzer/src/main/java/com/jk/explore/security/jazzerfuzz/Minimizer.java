package com.jk.explore.security.jazzerfuzz;

import java.util.function.Consumer;

/**
 * Makes a crashing input as small and plain as it can while it still crashes in the same place: it deletes stretches of characters, from half of the input down to one, then replaces each with a plainer one
 * (A for a letter, 0 for a digit), and repeats until nothing changes. A found input is a random-looking string; the minimal one is a sentence.
 */
public final class Minimizer {

    private Minimizer() {
    }

    public static String minimize(Consumer<String> parser, String input) {
        String signature = Crashes.signature(parser, input);
        if (signature == null) {
            return input;
        }
        String current = input;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int chunk = Math.max(1, current.length() / 2); chunk >= 1; chunk /= 2) {
                for (int i = 0; i + chunk <= current.length(); i++) {
                    String shorter = current.substring(0, i) + current.substring(i + chunk);
                    if (signature.equals(Crashes.signature(parser, shorter))) {
                        current = shorter;
                        changed = true;
                        i--;
                    }
                }
            }
            for (int i = 0; i < current.length(); i++) {
                char c = current.charAt(i);
                char plainer = Character.isDigit(c) ? '0' : Character.isUpperCase(c) ? 'A' : c;
                if (plainer != c) {
                    String simpler = current.substring(0, i) + plainer + current.substring(i + 1);
                    if (signature.equals(Crashes.signature(parser, simpler))) {
                        current = simpler;
                        changed = true;
                    }
                }
            }
        }
        return current;
    }
}
