package com.jk.explore.security.deser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A harmless-looking class from a "library already on the classpath": a task that removes a temporary file, and does it <em>while it is being read</em>.
 * Real gadget chains work the same way, but with classes from common libraries that end up running commands. Here the worst it can do is delete a marker file the demo made.
 */
public final class TempCleaner implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String path;

    public TempCleaner(String path) {
        this.path = path;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Files.deleteIfExists(Path.of(path)); // runs inside readObject(), before the caller even sees the object
    }
}
