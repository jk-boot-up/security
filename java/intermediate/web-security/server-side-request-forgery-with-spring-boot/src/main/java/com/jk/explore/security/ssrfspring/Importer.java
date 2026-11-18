package com.jk.explore.security.ssrfspring;

/** "Import a product image from a web address": fetch the address and return what it holds. */
interface Importer {

    String importImage(String address) throws ImportRefused;

    /** What the last import did, one line per step, as a security log would record it. */
    default java.util.List<String> trace() {
        return java.util.List.of();
    }
}
