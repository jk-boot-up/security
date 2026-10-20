package com.jk.explore.security.ssrf;

/** "Import a product image from a web address": the server fetches the address for the customer. */
public interface Importer {

    /** Returns the bytes of the image at the address. */
    String importImage(String address);
}
