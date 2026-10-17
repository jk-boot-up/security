package com.jk.explore.security.leastprivilegetf;

/** IAM-style wildcard matching: `*` matches any run of characters (including none) and `?` matches exactly one. */
final class Glob {

    private Glob() {
    }

    static boolean matches(String pattern, String text, boolean ignoreCase) {
        String p = ignoreCase ? pattern.toLowerCase(java.util.Locale.ROOT) : pattern;
        String t = ignoreCase ? text.toLowerCase(java.util.Locale.ROOT) : text;
        int pi = 0;
        int ti = 0;
        int star = -1;
        int mark = 0;
        while (ti < t.length()) {
            if (pi < p.length() && (p.charAt(pi) == '?' || p.charAt(pi) == t.charAt(ti))) {
                pi++;
                ti++;
            } else if (pi < p.length() && p.charAt(pi) == '*') {
                star = pi++;
                mark = ti;
            } else if (star >= 0) {
                pi = star + 1;
                ti = ++mark;
            } else {
                return false;
            }
        }
        while (pi < p.length() && p.charAt(pi) == '*') {
            pi++;
        }
        return pi == p.length();
    }
}
