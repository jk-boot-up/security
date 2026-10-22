package com.jk.explore.security.jacksondeser;

/** The cookies an attacker writes by hand. A browser sends back whatever is in the cookie, so any JSON is a possible cart. */
final class Attacker {

    private Attacker() {
    }

    /** The honest cart's JSON with the price of a kettle changed. */
    static String cheapCart() {
        return "{\"lines\":[{\"sku\":\"KTL-001\",\"qty\":2,\"price\":1}],\"gift\":{\"@class\":\"" + Note.Gift.class.getName() + "\",\"message\":\"hi\"}}";
    }

    /** A note that names a class instead of a kind. */
    static String classNameNote(String path) {
        return "{\"lines\":[],\"gift\":{\"@class\":\"" + TempCleaner.class.getName() + "\",\"path\":" + quote(path) + "}}";
    }

    /** The same idea against a field that is typed by a short name. */
    static String typeIdNote(String path) {
        return "{\"lines\":[],\"gift\":{\"type\":\"" + TempCleaner.class.getName() + "\",\"path\":" + quote(path) + "}}";
    }

    /** What a mapper with global default typing reads when the cookie is "any object": the class name, then the data. */
    static String defaultTypingRoot(String path) {
        return "[\"" + TempCleaner.class.getName() + "\",{\"path\":" + quote(path) + "}]";
    }

    static String deepArray(int depth) {
        return "[".repeat(depth) + "]".repeat(depth);
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
