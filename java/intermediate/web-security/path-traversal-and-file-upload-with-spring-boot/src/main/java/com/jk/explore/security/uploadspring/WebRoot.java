package com.jk.explore.security.uploadspring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** The web server's static files: whatever is in the folder is sent as it is, with a type chosen by the file name's extension. */
public final class WebRoot {

    private final Path folder;

    public WebRoot(Path folder) {
        this.folder = folder;
    }

    public Served serve(String path) {
        try {
            Path file = folder.resolve(path.startsWith("/") ? path.substring(1) : path).normalize();
            String name = file.getFileName().toString();
            String type = name.endsWith(".html") ? "text/html" : name.endsWith(".png") ? "image/png" : "application/octet-stream";
            return new Served(type, false, false, new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            throw new Refused("not found");
        }
    }
}
