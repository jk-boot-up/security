package com.jk.explore.security.uploadspring;

import java.io.IOException;
import java.nio.file.Files;

/** The second version: it does check the resolved path, but compares <em>text</em>, so a sibling folder with the same beginning passes. */
public final class PrefixDownloads implements Downloads {

    private final Layout layout;

    public PrefixDownloads(Layout layout) {
        this.layout = layout;
    }

    @Override
    public String read(String name) {
        try {
            var file = layout.invoices().resolve(name).normalize();
            if (!file.toString().startsWith(layout.invoices().toString())) { // "invoices-private/..." also starts with "invoices"
                throw new Refused("outside the invoice folder");
            }
            return Files.readString(file);
        } catch (IOException e) {
            throw new Refused("no such file");
        }
    }
}
