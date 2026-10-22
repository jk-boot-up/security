package com.jk.explore.security.uploadspring;

/** "Download invoice {name}": returns the text of the file. */
public interface Downloads {

    String read(String name);
}
