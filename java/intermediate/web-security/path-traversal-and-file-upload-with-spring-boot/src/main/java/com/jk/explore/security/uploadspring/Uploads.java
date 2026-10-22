package com.jk.explore.security.uploadspring;

/** "Upload a product photo": the customer sends a file name and bytes, and later fetches the photo. */
public interface Uploads {

    /** Returns where the file can be fetched from. */
    String save(String fileName, byte[] bytes);

    /** What the server sends back for a stored file, with the headers that go with it. */
    Served fetch(String name);
}
