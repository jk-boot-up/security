package com.jk.explore.security.files;

/** "Upload a product photo": the customer sends a file name and bytes. */
public interface Uploads {

    /** Returns where the file can be fetched from. */
    String save(String fileName, byte[] bytes);
}
