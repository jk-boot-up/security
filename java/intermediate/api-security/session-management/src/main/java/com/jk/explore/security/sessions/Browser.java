package com.jk.explore.security.sessions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** A browser's cookie jar, enough to follow what happens to the session cookie. */
final class Browser {

    private final Map<String, String> jar = new HashMap<>();

    /** Applies a Set-Cookie header: stores the value, or forgets the cookie when Max-Age=0. */
    void receive(String setCookie) {
        String first = setCookie.split(";")[0];
        int equals = first.indexOf('=');
        String name = first.substring(0, equals).strip();
        if (setCookie.toLowerCase(Locale.ROOT).contains("max-age=0")) {
            jar.remove(name);
        } else {
            jar.put(name, first.substring(equals + 1).strip());
        }
    }

    void put(String name, String value) {
        jar.put(name, value);
    }

    void clear(String name) {
        jar.remove(name);
    }

    Optional<String> get(String name) {
        return Optional.ofNullable(jar.get(name));
    }

    /** The Cookie header this browser sends, or nothing. */
    Optional<String> header() {
        return get(Cookies.NAME).map(v -> Cookies.NAME + "=" + v);
    }
}
