package com.jk.explore.security.files;

/** "Download invoice {name}": returns the text of the file. */
public interface Downloads {

    String read(String name);
}
