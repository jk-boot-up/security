package com.jk.explore.security.authspring;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * How HTTP Basic carries a login: the username and password joined by a colon, then written in Base64.
 * Base64 is an encoding, not encryption: anyone who sees the header can turn it back.
 */
public final class BasicHeader {

    private BasicHeader() {
    }

    public static String joined(String user, String password) {
        return user + ":" + password;
    }

    public static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /** The value of the Authorization header the client sends. */
    public static String header(String user, String password) {
        return "Basic " + base64(joined(user, password));
    }

    public static String decode(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    /** Base64 step by step: every 3 bytes in become 4 letters out. Each pair is {3 bytes, 4 letters}. */
    public static List<String[]> steps(String text) {
        List<String[]> out = new ArrayList<>();
        for (int i = 0; i < text.length(); i += 3) {
            String group = text.substring(i, Math.min(i + 3, text.length()));
            out.add(new String[] {group, base64(group)});
        }
        return out;
    }
}
