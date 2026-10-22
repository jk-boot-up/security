package com.jk.explore.security.iammodel;

import java.util.regex.Pattern;

/** The wildcards of policy documents: * matches any run of characters (including none) and ? exactly one. */
final class Wild {

    private Wild() {
    }

    static boolean matches(String pattern, String value, boolean ignoreCase) {
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                default -> regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString(), ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.DOTALL : Pattern.DOTALL).matcher(value).matches();
    }
}
