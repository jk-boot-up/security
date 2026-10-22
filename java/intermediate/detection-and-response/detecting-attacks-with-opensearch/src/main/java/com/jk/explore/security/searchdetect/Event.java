package com.jk.explore.security.searchdetect;

import java.time.Instant;

/**
 * One line of the audit log. {@code type} is "login" or "role-grant". For a grant, {@code actor} gives the role to {@code user} and {@code ticket} is the change
 * request that approved it (empty if none). {@code attackId} is the ground truth, used only to score rules: a real detector never sees it.
 */
public record Event(Instant time, String type, String user, String ip, City city, boolean success, String actor, String role, String ticket, String attackId) {

    public static Event login(Instant t, String user, String ip, City city, boolean ok, String attackId) {
        return new Event(t, "login", user, ip, city, ok, user, "", "", attackId);
    }

    public static Event grant(Instant t, String actor, String user, String role, String ticket, String attackId) {
        return new Event(t, "role-grant", user, "10.0.0.1", City.PARIS, true, actor, role, ticket, attackId);
    }
}
