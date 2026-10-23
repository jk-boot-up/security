package com.jk.explore.security.sast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** The truth about a line of the code under test, written in a comment: {@code // VULN sql-direct} or {@code // SAFE sql-prepared}. */
public record Marker(String file, int line, boolean vulnerable, String id) {

    private static final Pattern MARK = Pattern.compile("// (VULN|SAFE) ([a-z-]+)");

    public static List<Marker> in(Path dir) throws IOException {
        List<Marker> markers = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                List<String> lines = Files.readAllLines(p);
                for (int i = 0; i < lines.size(); i++) {
                    Matcher m = MARK.matcher(lines.get(i));
                    if (m.find()) {
                        markers.add(new Marker(p.getFileName().toString(), i + 1, m.group(1).equals("VULN"), m.group(2)));
                    }
                }
            }
        }
        return markers;
    }

    String key() {
        return file + ":" + line;
    }
}
