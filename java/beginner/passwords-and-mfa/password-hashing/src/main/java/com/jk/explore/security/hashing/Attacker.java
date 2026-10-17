package com.jk.explore.security.hashing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Someone who has stolen the table of stored passwords and tries a list of common passwords against it. */
public class Attacker {

    private final List<String> wordlist;

    public Attacker(List<String> wordlist) {
        this.wordlist = wordlist;
    }

    /** Hashes every word once and looks each stolen value up in that table. Works only when hashes carry no salt. */
    public Cracked crackWithTable(Map<String, String> stolen, PasswordHasher hasher) {
        Map<String, String> table = new HashMap<>();
        for (String word : wordlist) {
            table.put(hasher.hash(word), word);
        }
        Map<String, String> found = new LinkedHashMap<>();
        stolen.forEach((user, stored) -> {
            if (table.containsKey(stored)) {
                found.put(user, table.get(stored));
            }
        });
        return new Cracked(found, wordlist.size());
    }

    /** Tries the words in order against each account on its own, as is needed when every hash has its own salt. */
    public Cracked crackEachAccount(Map<String, String> stolen, PasswordHasher hasher) {
        Map<String, String> found = new LinkedHashMap<>();
        int guesses = 0;
        for (Map.Entry<String, String> account : stolen.entrySet()) {
            for (String word : wordlist) {
                guesses++;
                if (hasher.verify(word, account.getValue())) {
                    found.put(account.getKey(), word);
                    break;
                }
            }
        }
        return new Cracked(found, guesses);
    }
}
