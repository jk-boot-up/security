package com.jk.explore.security.sessionredis;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * How Spring Session names a session in Redis and in the cookie, worked out by hand so the demo can show it and the tests can check it
 * without Redis. The cookie is the Base64 of the session id, and the id is part of the Redis key as it is: Redis holds live ids.
 */
final class SessionKeys {

    static final String SESSIONS = "spring:session:sessions:";
    static final String EXPIRES = SESSIONS + "expires:";
    private static final String INDEX_CLASS = "org.springframework.session.FindByIndexNameSessionRepository.";

    private SessionKeys() {
    }

    /** The cookie value for a session id: the Base64 of the id's characters. */
    static String cookieFor(String id) {
        byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** The standard Base64 alphabet: the character for each 6-bit value, 0 to 63. */
    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /** Base64 by hand, for text whose length is a multiple of 3: each character's 8 bits in one row, cut into 6-bit groups. */
    static List<String> sixBitGroups(String text) {
        StringBuilder bits = new StringBuilder();
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            bits.append(String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(' ', '0'));
        }
        List<String> groups = new ArrayList<>();
        for (int i = 0; i < bits.length(); i += 6) {
            groups.add(bits.substring(i, i + 6));
        }
        return groups;
    }

    /** The same groups, each turned into its character: equal to {@link #cookieFor} when the length is a multiple of 3. */
    static String byHand(String text) {
        StringBuilder out = new StringBuilder();
        for (String group : sixBitGroups(text)) {
            out.append(ALPHABET.charAt(Integer.parseInt(group, 2)));
        }
        return out.toString();
    }

    /** The session id inside a cookie value. */
    static String idFrom(String cookie) {
        return new String(Base64.getDecoder().decode(cookie), StandardCharsets.UTF_8);
    }

    /** The Redis key that holds a session's fields. */
    static String recordKey(String id) {
        return SESSIONS + id;
    }

    /**
     * The live session records among Redis keys: a record counts while its expiry marker exists. Spring Session leaves the data of an
     * ended session in Redis for five more minutes, with no marker, and the bank no longer opens it.
     */
    static Set<String> live(Set<String> keys) {
        return keys.stream().filter(k -> k.startsWith(SESSIONS) && !k.startsWith(EXPIRES))
                .filter(k -> keys.contains(EXPIRES + k.substring(SESSIONS.length())))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** A Redis key as the demo prints it: the id as {@code <id>}, an expiry time as {@code <time>}, the long index class name cut short. */
    static String shown(String key, String id) {
        return key.replace(id, "<id>").replace(INDEX_CLASS, "").replaceAll(":\\d{10,}$", ":<time>");
    }

    /** What is wrong with the cookie the bank set. Empty means nothing. */
    static List<String> cookieProblems(String setCookie) {
        List<String> problems = new ArrayList<>();
        if (!setCookie.startsWith("__Host-")) {
            problems.add("no __Host- prefix");
        }
        if (!setCookie.contains("Secure")) {
            problems.add("no Secure");
        }
        if (!setCookie.contains("HttpOnly")) {
            problems.add("no HttpOnly");
        }
        if (!setCookie.contains("SameSite=Strict")) {
            problems.add("no SameSite=Strict");
        }
        if (!setCookie.contains("Path=/") || setCookie.contains("Domain=")) {
            problems.add("path or domain");
        }
        return problems;
    }
}
