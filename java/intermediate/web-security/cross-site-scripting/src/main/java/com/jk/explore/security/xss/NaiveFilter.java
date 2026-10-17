package com.jk.explore.security.xss;

/**
 * The tempting fix that is not one: cleaning input on the way in by deleting characters. It does not know where the text
 * will be written, so it removes too much from honest text and too little for the places it was never designed for.
 */
final class NaiveFilter {

    private NaiveFilter() {
    }

    static String clean(String text) {
        return text.replace("<", "").replace(">", "");
    }
}
