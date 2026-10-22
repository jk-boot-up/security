package com.jk.explore.security.searchdetect;

import java.util.Set;

/** One alert from a query: the subject (an account, an address), the window it fell in (or empty), how many events made it, and the real attacks behind it (scoring only). */
public record Hit(String subject, String window, long events, Set<String> attacks) {

    public boolean truePositive() {
        return !attacks.isEmpty();
    }
}
