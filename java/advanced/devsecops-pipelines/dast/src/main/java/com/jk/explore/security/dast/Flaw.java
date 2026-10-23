package com.jk.explore.security.dast;

import java.util.List;

/** A flaw planted in the application: what a scanner should raise, and on which path ({@code null} for any path). */
public record Flaw(String id, String alert, String path, boolean needsLogin) {

    static final List<Flaw> PLANTED = List.of(
            new Flaw("reflected page", "Cross Site Scripting (Reflected)", "/search", false),
            new Flaw("error message echoes input", "Cross Site Scripting (Reflected)", "/account", false),
            new Flaw("reflected page in the admin area", "Cross Site Scripting (Reflected)", "/admin/report", true),
            new Flaw("query built with a string", "SQL Injection", "/account", false),
            new Flaw("session cookie without HttpOnly", "Cookie No HttpOnly Flag", "/login", false),
            new Flaw("session cookie without SameSite", "Cookie without SameSite Attribute", "/login", false),
            new Flaw("no Content-Security-Policy", "Content Security Policy (CSP) Header Not Set", null, false),
            new Flaw("no clickjacking protection", "Missing Anti-clickjacking Header", null, false),
            new Flaw("no nosniff", "X-Content-Type-Options Header Missing", null, false));

    boolean raisedIn(List<Alert> alerts) {
        return alerts.stream().anyMatch(a -> a.name().startsWith(alert) && (path == null || a.on(path)));
    }
}
