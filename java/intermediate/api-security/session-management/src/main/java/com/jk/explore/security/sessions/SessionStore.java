package com.jk.explore.security.sessions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Where sessions live, keyed by the hash of their id. One store per server is enough for one server; several servers need one shared
 * store, which is what Spring Session with Redis provides.
 */
final class SessionStore {

    private final Map<String, Session> sessions = new LinkedHashMap<>();

    synchronized void put(String key, Session session) {
        sessions.put(key, session);
    }

    synchronized Session get(String key) {
        return sessions.get(key);
    }

    synchronized void remove(String key) {
        sessions.remove(key);
    }

    /** Removes every session that matches, and returns how many were removed. */
    synchronized int removeIf(Predicate<Map.Entry<String, Session>> test) {
        int before = sessions.size();
        sessions.entrySet().removeIf(test);
        return before - sessions.size();
    }

    /** The keys, oldest first. */
    synchronized List<String> keys() {
        return new ArrayList<>(sessions.keySet());
    }

    synchronized Set<String> keySet() {
        return Set.copyOf(sessions.keySet());
    }

    synchronized int size() {
        return sessions.size();
    }
}
