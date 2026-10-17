package com.jk.explore.security.attacks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The three ways of guessing online, each as a short script run against a bank's login. */
public class Attacks {

    private final Bank bank;
    private final Ticker ticker;

    public Attacks(Bank bank, Ticker ticker) {
        this.bank = bank;
        this.ticker = ticker;
    }

    /**
     * A dictionary attack: one account, many passwords, one guess per tick. An attempt turned away by a lock or a
     * limit is tried again on the next tick, and does not use up a word.
     */
    public Found dictionary(String address, String username, List<String> words) {
        Map<String, Integer> taken = new LinkedHashMap<>();
        int guesses = 0;
        int next = 0;
        while (next < words.size()) {
            Reply reply = bank.login(address, username, words.get(next));
            if (reply.outcome() != Outcome.LOCKED && reply.outcome() != Outcome.RATE_LIMITED) {
                guesses++;
                next++;
                if (reply.outcome() == Outcome.OK) {
                    taken.put(username, ticker.now());
                    return finish(taken, guesses);
                }
            }
            ticker.advance(1);
        }
        return finish(taken, guesses);
    }

    /**
     * Password spraying: a few very common passwords, each tried once against every account. No account sees more than
     * a handful of failures, so a lockout per account never triggers.
     */
    public Found spray(String address, List<String> usernames, List<String> passwords) {
        Map<String, Integer> taken = new LinkedHashMap<>();
        int guesses = 0;
        for (String password : passwords) {
            for (String username : usernames) {
                if (taken.containsKey(username)) {
                    continue;
                }
                boolean done = false;
                while (!done) {
                    Reply reply = bank.login(address, username, password);
                    if (reply.outcome() != Outcome.LOCKED && reply.outcome() != Outcome.RATE_LIMITED) {
                        guesses++;
                        done = true;
                        if (reply.outcome() == Outcome.OK) {
                            taken.put(username, ticker.now());
                        }
                    }
                    ticker.advance(1);
                }
            }
        }
        return finish(taken, guesses);
    }

    /**
     * Credential stuffing: pairs of username and password leaked from another site, each tried once, each from a
     * different address, all in the same tick. Returns how many replies of each kind came back.
     */
    public Map<Outcome, Integer> stuff(Map<String, String> leakedPairs) {
        Map<Outcome, Integer> counts = new LinkedHashMap<>();
        int address = 0;
        for (Map.Entry<String, String> pair : leakedPairs.entrySet()) {
            Reply reply = bank.login("bot-" + (++address), pair.getKey(), pair.getValue());
            counts.merge(reply.outcome(), 1, Integer::sum);
        }
        return counts;
    }

    private Found finish(Map<String, Integer> taken, int guesses) {
        return new Found(taken, guesses, ticker.now());
    }
}
