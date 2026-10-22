package com.jk.explore.security.jazzerfuzz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/** One distinct problem that Jazzer reported: the kind of exception, where in the store's code it happened, and the file that holds the input. */
public record Finding(String exception, String where, String token, Path input) {

    /** For example "ArrayIndexOutOfBoundsException in OrderLine.parseCareless (line 24)". */
    public String describe() {
        return exception + " in " + where;
    }

    /** The saved input as the target saw it: the fuzz targets read their bytes as ASCII, seven bits each. */
    public String inputText() throws IOException {
        byte[] bytes = Files.readAllBytes(input);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] &= 0x7f;
        }
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}
