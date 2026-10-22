package com.jk.explore.security.ssrfspring;

/** "Import a product image from a web address": fetch the address and return what it holds. */
interface Importer {

    String importImage(String address) throws ImportRefused;
}
