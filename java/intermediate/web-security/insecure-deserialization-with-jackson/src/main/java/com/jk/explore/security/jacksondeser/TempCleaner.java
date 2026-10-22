package com.jk.explore.security.jacksondeser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A harmless stand-in for a gadget: a class that is already on the classpath (here a note from another team's plug-in, in real life
 * a library's class) and does something as a side effect of being built from data. Setting its path removes that file.
 */
public class TempCleaner implements Note {

    public void setPath(String path) {
        try {
            Files.deleteIfExists(Path.of(path));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
