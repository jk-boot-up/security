package com.jk.explore.security.leaks;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** A teaching class: reads an error page the way a curious visitor would, and lists what it gives away. */
public final class PageReader {

    private static final Pattern EXCEPTION = Pattern.compile("^([\\w.]+(?:Exception|Error))");
    private static final Pattern JDBC = Pattern.compile("jdbc:\\w+://([^:/\\s]+):(\\d+)/(\\w+)\\?user=(\\w+)&password=([^\\s&]+)");
    private static final Pattern FRAME = Pattern.compile("at [\\w.]+\\.(\\w+\\.\\w+)");

    private PageReader() {
    }

    public static Map<String, String> read(String page) {
        Map<String, String> found = new LinkedHashMap<>();
        Matcher e = EXCEPTION.matcher(page);
        if (e.find()) {
            found.put("exception", e.group(1));
        }
        Matcher u = JDBC.matcher(page);
        if (u.find()) {
            found.put("host", u.group(1));
            found.put("port", u.group(2));
            found.put("database", u.group(3));
            found.put("user", u.group(4));
            found.put("password", u.group(5));
        }
        String frames = FRAME.matcher(page).results().map(m -> m.group(1)).collect(Collectors.joining(", "));
        if (!frames.isEmpty()) {
            found.put("code", frames);
        }
        return found;
    }
}
