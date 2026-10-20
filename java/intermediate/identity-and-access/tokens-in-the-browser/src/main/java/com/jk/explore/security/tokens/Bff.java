package com.jk.explore.security.tokens;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * A backend for the frontend at {@code https://app.bank.example}. It signs the customer in, keeps the tokens in its own memory, and gives the browser only a
 * session cookie ({@code __Host-sid}, HttpOnly, Secure, SameSite=Lax). The page asks the BFF, and the BFF calls the API with the token.
 */
public final class Bff {

    public static final String COOKIE = "__Host-sid";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> tokenBySession = new HashMap<>();
    private final ResourceApi api;
    private final AuthServer auth;

    public Bff(AuthServer auth, ResourceApi api) {
        this.auth = auth;
        this.api = api;
    }

    /** After a successful sign-in the BFF gets a token for the customer, keeps it, and sets a cookie. Returns the cookie value. */
    public String signIn(String user) {
        byte[] b = new byte[24];
        random.nextBytes(b);
        String sid = B64.encode(b);
        tokenBySession.put(sid, auth.issue(user, null));
        return sid;
    }

    /**
     * A request from the page. A state-changing request must carry the custom header and come from the app's own origin: a form post from another
     * site can add neither, though it would carry the cookie.
     */
    public ApiReply handle(String method, String path, String cookieHeader, String origin, String customHeader) {
        String sid = cookieValue(cookieHeader);
        String token = sid == null ? null : tokenBySession.get(sid);
        if (token == null) {
            return new ApiReply(401, "not signed in");
        }
        boolean changesState = !method.equals("GET");
        if (changesState && (!"https://app.bank.example".equals(origin) || !"fetch".equals(customHeader))) {
            return new ApiReply(403, "cross-site request refused");
        }
        return api.call(method, path, "Bearer " + token, null);
    }

    /** Sign-out removes the tokens, so a copied session id is dead at once. */
    public void signOut(String sid) {
        tokenBySession.remove(sid);
    }

    private static String cookieValue(String header) {
        if (header == null) {
            return null;
        }
        for (String part : header.split(";\\s*")) {
            if (part.startsWith(COOKIE + "=")) {
                return part.substring(COOKIE.length() + 1);
            }
        }
        return null;
    }
}
