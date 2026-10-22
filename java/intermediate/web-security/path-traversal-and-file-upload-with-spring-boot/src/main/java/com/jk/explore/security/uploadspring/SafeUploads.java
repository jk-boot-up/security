package com.jk.explore.security.uploadspring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The fixed version: size limit, the type decided from the bytes and not from the name, a name the <em>server</em> makes, storage outside the web root,
 * and downloads that name the type, force a download and forbid the browser to guess.
 */
public final class SafeUploads implements Uploads {

    static final int MAX_BYTES = 1000;

    private final Layout layout;
    private final Map<String, String> typeById = new HashMap<>();
    private int counter;

    public SafeUploads(Layout layout) {
        this.layout = layout;
    }

    @Override
    public String save(String ignoredFileName, byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new Refused("size not allowed");
        }
        Sniffer.Kind kind = Sniffer.of(bytes);
        if (kind == null) {
            throw new Refused("not a PNG or JPEG image");
        }
        String id = String.format("photo-%04d.%s", ++counter, kind.extension()); // the customer's file name is not used for anything
        Path target = layout.uploadsPrivate().resolve(id);
        try {
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new Refused("could not save");
        }
        typeById.put(id, kind.contentType());
        return "/uploads/" + id;
    }

    /** Fetches an upload by the id this class made. */
    @Override
    public Served fetch(String id) {
        String type = typeById.get(id);
        if (type == null) {
            throw new Refused("not found"); // only ids we created exist, so there is no path to traverse
        }
        try {
            return new Served(type, true, true, new String(Files.readAllBytes(layout.uploadsPrivate().resolve(id)), StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            throw new Refused("not found");
        }
    }
}
