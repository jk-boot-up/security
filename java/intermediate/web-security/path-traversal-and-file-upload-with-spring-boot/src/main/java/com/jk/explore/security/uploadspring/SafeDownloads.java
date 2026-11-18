package com.jk.explore.security.uploadspring;

import java.io.IOException;
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

    /**
     * {@code name} has already been decoded once, by Spring, as a {@code @RequestParam}. It is never decoded again here:
     * a second decode would turn {@code %252e} into a dot after the checks were meant to see it.
     */
    @Override
    public String read(String name) {
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
