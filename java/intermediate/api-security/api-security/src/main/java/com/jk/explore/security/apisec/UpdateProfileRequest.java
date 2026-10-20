package com.jk.explore.security.apisec;

import java.util.Map;
import java.util.Set;

/** The only things a customer may change about an account, read from the JSON by name, with unknown fields refused rather than ignored. */
public record UpdateProfileRequest(String displayName, String email) {

    private static final Set<String> FIELDS = Set.of("displayName", "email");

    public static UpdateProfileRequest parse(Map<String, Object> body) {
        for (String key : body.keySet()) {
            if (!FIELDS.contains(key)) {
                throw new BadRequest("unknown field");
            }
        }
        String name = text(body, "displayName", 40);
        String email = text(body, "email", 80);
        if (email != null && !email.matches("[^@\\s]+@[^@\\s]+")) {
            throw new BadRequest("bad email");
        }
        if (name == null && email == null) {
            throw new BadRequest("nothing to change");
        }
        return new UpdateProfileRequest(name, email);
    }

    private static String text(Map<String, Object> body, String key, int max) {
        Object v = body.get(key);
        if (v == null) {
            return null;
        }
        if (!(v instanceof String s) || s.isBlank() || s.length() > max) {
            throw new BadRequest("bad " + key);
        }
        return s;
    }

    /** A request the API refuses with a 400. */
    public static final class BadRequest extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public BadRequest(String message) {
            super(message);
        }
    }
}
