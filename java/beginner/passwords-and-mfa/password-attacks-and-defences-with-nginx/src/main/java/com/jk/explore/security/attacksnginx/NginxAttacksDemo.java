package com.jk.explore.security.attacksnginx;

import java.io.IOException;

/** Runs the online bank story in five numbered acts against real nginx containers and prints repeatable results. */
public class NginxAttacksDemo {

    static final int ATTEMPTS = 20;
    static final String RATE = "1r/m";
    static final int BURST = 5;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text. It needs a running Docker, so its test is tagged "docker". */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1NoLimit(out);
        act2LimitByAddress(out);
        act3LimitByHeader(out);
        act4LimitByRealAddress(out);
        act5LimitByUsername(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String describe(Tally t) {
        return t.answered() + " answered, " + t.refused() + " refused with 429";
    }

    private static void act1NoLimit(StringBuilder out) throws IOException, InterruptedException {
        line(out, "ONE. nginx in front of the login, with no limit.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Key.NONE, RATE, BURST))) {
            Tally t = new Attacker(nginx.port()).run(ATTEMPTS, i -> "dana", i -> null);
            line(out, "  " + ATTEMPTS + " guesses at dana's login: " + describe(t) + "  <- ANYONE CAN KEEP GUESSING");
        }
    }

    private static void act2LimitByAddress(StringBuilder out) throws IOException, InterruptedException {
        line(out, "TWO. limit_req on the client's address: " + RATE + ", burst " + BURST + ".");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Key.ADDRESS, RATE, BURST))) {
            Tally t = new Attacker(nginx.port()).run(ATTEMPTS, i -> "dana", i -> null);
            line(out, "  " + ATTEMPTS + " guesses at dana's login: " + describe(t));
            line(out, "  the first " + t.answered() + " get through (1 plus the burst of " + BURST + "), the rest are turned away");
        }
    }

    private static void act3LimitByHeader(StringBuilder out) throws IOException, InterruptedException {
        line(out, "THREE. The same limit, but counted on the X-Forwarded-For header.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Key.FORWARDED_HEADER, RATE, BURST))) {
            Tally t = new Attacker(nginx.port()).run(ATTEMPTS, i -> "dana", i -> "10.0.0." + i);
            line(out, "  " + ATTEMPTS + " guesses, each with a different X-Forwarded-For value: " + describe(t)
                    + "  <- THE ATTACKER CHOSE ITS OWN KEY");
        }
    }

    private static void act4LimitByRealAddress(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. The same limit, counted on the real connection address.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Key.ADDRESS, RATE, BURST))) {
            Tally t = new Attacker(nginx.port()).run(ATTEMPTS, i -> "dana", i -> "10.0.0." + i);
            line(out, "  " + ATTEMPTS + " guesses with different X-Forwarded-For values: " + describe(t));
            line(out, "  the header is ignored, so changing it buys nothing");
        }
    }

    private static void act5LimitByUsername(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The bill: a limit per username.");
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Key.USERNAME, RATE, BURST))) {
            Attacker attacker = new Attacker(nginx.port());
            Tally onDana = attacker.run(ATTEMPTS, i -> "dana", i -> null);
            line(out, "  " + ATTEMPTS + " guesses at dana: " + describe(onDana));
            int dana = attacker.attempt("dana", "203.0.113.7");
            line(out, "  dana herself tries from another address: " + dana + "  <- THE LIMIT LOCKS THE OWNER OUT TOO");
        }
        try (Nginx nginx = Nginx.start(NginxConfig.text(NginxConfig.Key.USERNAME, RATE, BURST))) {
            Tally spray = new Attacker(nginx.port()).run(ATTEMPTS, i -> "customer" + i, i -> null);
            line(out, "  one guess at each of " + ATTEMPTS + " usernames: " + describe(spray)
                    + "  <- A SPRAY NEVER REACHES THE LIMIT");
        }
    }
}
