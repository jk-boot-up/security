package com.jk.explore.security.hashingspring;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Someone with a stolen table of stored passwords, trying a list of common passwords against each account. */
public class Attacker {

    /** What the attacker found, and how many guesses it took. */
    public record Cracked(Map<String, String> found, int guesses) {
    }

    private final List<String> wordlist;

    public Attacker(List<String> wordlist) {
        this.wordlist = wordlist;
    }

    /** Tries the words in order against each account on its own, using the encoder's own check. */
    public Cracked crack(Map<String, String> stolen, PasswordEncoder encoder) {
        Map<String, String> found = new LinkedHashMap<>();
        int guesses = 0;
        for (Map.Entry<String, String> account : stolen.entrySet()) {
            for (String word : wordlist) {
                guesses++;
                if (encoder.matches(word, account.getValue())) {
                    found.put(account.getKey(), word);
                    break;
                }
            }
        }
        return new Cracked(found, guesses);
    }
}
