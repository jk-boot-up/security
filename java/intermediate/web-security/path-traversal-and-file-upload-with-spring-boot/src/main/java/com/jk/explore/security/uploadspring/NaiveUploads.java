package com.jk.explore.security.uploadspring;

import java.io.IOException;
import java.nio.file.Files;

/** The first version: the file is saved in the web root under the name the customer gave, whatever it contains. */
public final class NaiveUploads implements Uploads {

    private final Layout layout;

    public NaiveUploads(Layout layout) {
        this.layout = layout;
    }

    @Override
    public Served fetch(String name) {
        return new WebRoot(layout.webroot()).serve("/uploads/" + name);
    }

    @Override
    public String save(String fileName, byte[] bytes) {
        try {
            Files.write(layout.webroot().resolve("uploads").resolve(fileName), bytes);
            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new Refused("could not save");
        }
    }
}
