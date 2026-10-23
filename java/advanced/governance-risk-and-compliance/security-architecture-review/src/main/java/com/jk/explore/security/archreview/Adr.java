package com.jk.explore.security.archreview;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** The architecture decision records of a design: a number, a title and a status. */
public record Adr(String number, String title, String status) {

    private static final Pattern TITLE = Pattern.compile("^# (\\d+)\\. (.+)$", Pattern.MULTILINE);
    private static final Pattern STATUS = Pattern.compile("^Status: (.+)$", Pattern.MULTILINE);

    /** The records in a folder, by number as it is written in the file name: 0001, 0002, and so on. */
    public static Map<String, Adr> load(Path dir) throws IOException {
        Map<String, Adr> adrs = new HashMap<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".md")).toList()) {
                String text = Files.readString(p);
                Matcher t = TITLE.matcher(text);
                Matcher s = STATUS.matcher(text);
                if (t.find() && s.find()) {
                    adrs.put(String.format("%04d", Integer.parseInt(t.group(1))), new Adr(t.group(1), t.group(2), s.group(1)));
                }
            }
        }
        return adrs;
    }

    public boolean accepted() {
        return status.equals("Accepted");
    }
}
