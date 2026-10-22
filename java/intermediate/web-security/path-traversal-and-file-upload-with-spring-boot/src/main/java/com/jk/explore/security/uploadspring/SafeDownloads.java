package com.jk.explore.security.uploadspring;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * The fixed version, in layers: the name must look like an invoice name, the path is resolved and compared as a <em>path</em> (element by element),
 * and the real file, after symbolic links are followed, must still be inside the real folder.
 */
public final class SafeDownloads implements Downloads {

    private static final Pattern NAME = Pattern.compile("invoice-[0-9]{4}\\.txt");

    private final Layout layout;

    public SafeDownloads(Layout layout) {
        this.layout = layout;
    }

    /** {@code rawName} is what the web layer received, still percent-encoded; it is decoded exactly once, here, and then checked. */
    @Override
    public String read(String rawName) {
        String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
        if (!NAME.matcher(name).matches()) {
            throw new Refused("not an invoice name");
        }
        try {
            Path base = layout.invoices().toRealPath();
            Path file = base.resolve(name).normalize();
            if (!file.startsWith(base)) {
                throw new Refused("outside the invoice folder");
            }
            Path real = file.toRealPath(); // follows links: a link inside the folder may point outside it
            if (!real.startsWith(base)) {
                throw new Refused("link leads outside the invoice folder");
            }
            return Files.readString(real);
        } catch (IOException e) {
            throw new Refused("no such file");
        }
    }
}
