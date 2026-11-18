package com.jk.explore.security.sbomscan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compares versions as numbers, so that 2.9 sorts before 2.17, the way an advisory's "fixed in" version is compared with an installed one. */
final class Versions {

    private Versions() {
    }

    static int compare(String a, String b) {
        List<Integer> x = numbers(a);
        List<Integer> y = numbers(b);
        for (int i = 0; i < Math.max(x.size(), y.size()); i++) {
            int c = Integer.compare(i < x.size() ? x.get(i) : 0, i < y.size() ? y.get(i) : 0);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    static List<Integer> numbers(String v) {
        List<Integer> parts = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+").matcher(v);
        while (m.find()) {
            parts.add(Integer.parseInt(m.group()));
        }
        return parts;
    }
}
