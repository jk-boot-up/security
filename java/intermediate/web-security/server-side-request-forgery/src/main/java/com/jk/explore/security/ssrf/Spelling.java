package com.jk.explore.security.ssrf;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out by hand the real address behind the unusual spellings a blocklist of names misses. The JDK does the same silently when it
 * connects; the tests prove these steps give the JDK's exact answer.
 */
public final class Spelling {

    /** The steps of the working, and the four-part address they lead to. */
    public record Worked(List<String> steps, String address) {
    }

    private Spelling() {
    }

    /** One number for the whole address: divide by 256 x 256 x 256, then 256 x 256, then 256; each quotient is one part. */
    public static Worked decimal(long number) {
        List<String> steps = new ArrayList<>();
        long[] parts = new long[4];
        long rest = number;
        for (int i = 0; i < 3; i++) {
            long weight = 1L << (8 * (3 - i));
            parts[i] = rest / weight;
            steps.add(rest + " / " + weight + " = " + parts[i] + ", remainder " + rest % weight);
            rest = rest % weight;
        }
        parts[3] = rest;
        return new Worked(steps, dotted(parts));
    }

    /** Fewer than four parts: the last number fills all the parts that are missing. */
    public static Worked shortForm(String text) {
        String[] given = text.split("\\.");
        long[] parts = new long[4];
        for (int i = 0; i < given.length - 1; i++) {
            parts[i] = Long.parseLong(given[i]);
        }
        long last = Long.parseLong(given[given.length - 1]);
        int missing = 4 - given.length;
        for (int i = 3; i >= given.length - 1; i--) {
            parts[i] = last % 256;
            last = last / 256;
        }
        List<String> steps = List.of(given.length + " parts given, " + missing + " missing",
                "the last number, " + given[given.length - 1] + ", fills the last " + (missing + 1) + " parts");
        return new Worked(steps, dotted(parts));
    }

    /** An IPv4 address inside IPv6: after ::ffff: come two groups of four hex digits, two parts each. */
    public static Worked mapped(String text) {
        String hex = text.substring(text.lastIndexOf("ffff:") + 5).replace(":", "");
        long[] parts = new long[4];
        List<String> steps = new ArrayList<>();
        steps.add("::ffff: means an IPv4 address follows");
        for (int g = 0; g < 2; g++) {
            String a = hex.substring(4 * g, 4 * g + 2);
            String b = hex.substring(4 * g + 2, 4 * g + 4);
            parts[2 * g] = Long.parseLong(a, 16);
            parts[2 * g + 1] = Long.parseLong(b, 16);
            steps.add(a + b + ": hex " + a + " = " + parts[2 * g] + ", hex " + b + " = " + parts[2 * g + 1]);
        }
        return new Worked(steps, dotted(parts));
    }

    private static String dotted(long[] parts) {
        return parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
    }
}
