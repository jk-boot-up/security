package com.jk.explore.security.files;

import java.io.IOException;
import java.nio.file.Files;

/** The first version: the file is whatever the base folder plus the name says, {@code ..} included. */
public final class NaiveDownloads implements Downloads {

    private final Layout layout;

    public NaiveDownloads(Layout layout) {
        this.layout = layout;
    }

    @Override
    public String read(String name) {
        try {
            return Files.readString(layout.invoices().resolve(name));
        } catch (IOException e) {
            throw new Refused("no such file");
        }
    }
}
