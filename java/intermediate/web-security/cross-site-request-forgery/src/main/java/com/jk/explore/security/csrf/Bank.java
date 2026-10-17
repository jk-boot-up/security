package com.jk.explore.security.csrf;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An online bank with cookie sessions and a transfer form, and the CSRF defences that {@link Protection} switches on. The bank's
 * own pages are served from {@link #ORIGIN}. Session ids, form tokens and API tokens are random; nothing prints them.
 */
final class Bank {

    static final String ORIGIN = "https://bank.example";

    private record Session(String user, String token) {
    }

    private final Protection protection;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> passwords = new HashMap<>();
    private final Map<String, Integer> balances = new HashMap<>();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, String> apiTokens = new HashMap<>();
    private int transfers;

    Bank(Protection protection) {
        this.protection = protection;
        open("ana", "correct horse battery staple", 1000);
        open("mallory", "mallory-demo-password", 0);
    }

    private void open(String user, String password, int balance) {
        passwords.put(user, password);
        balances.put(user, balance);
    }

    int balance(String user) {
        return balances.get(user);
    }

    int transfers() {
        return transfers;
    }

    /** A token for the bank's API, sent in an Authorization header: a browser never attaches it on its own. */
    String issueApiToken(String user) {
        String token = randomText();
        apiTokens.put(token, user);
        return token;
    }

    private String randomText() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean sameSecret(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    synchronized Response handle(Request r) {
        return switch (r.path()) {
            case "/login" -> login(r);
            case "/account" -> session(r).map(s -> Response.of(200, "balance " + balances.get(s.user())))
                    .orElse(Response.of(401, "not logged in"));
            case "/transfer-form" -> session(r).map(this::form).orElse(Response.of(401, "not logged in"));
            case "/transfer" -> transfer(r);
            case "/api/transfer" -> apiTransfer(r);
            default -> Response.of(404, "no such page");
        };
    }

    private Response login(Request r) {
        String user = r.form().get("user");
        if (!"POST".equals(r.method()) || user == null || !sameSecret(passwords.get(user), r.form().get("password"))) {
            return Response.of(401, "wrong username or password");
        }
        String id = randomText();
        sessions.put(id, new Session(user, randomText()));
        String cookie = "sid=" + id + "; Path=/; Secure; HttpOnly" + (protection.sameSite() == null ? "" : "; SameSite=" + protection.sameSite());
        return new Response(200, "welcome " + user, Map.of("Set-Cookie", cookie));
    }

    private Optional<Session> session(Request r) {
        String cookies = r.header("cookie");
        if (cookies == null) {
            return Optional.empty();
        }
        for (String part : cookies.split(";")) {
            String p = part.strip();
            if (p.startsWith("sid=")) {
                return Optional.ofNullable(sessions.get(p.substring(4)));
            }
        }
        return Optional.empty();
    }

    private Response form(Session s) {
        String hidden = protection.tokens() ? "<input type=\"hidden\" name=\"csrf\" value=\"" + s.token() + "\">" : "";
        return Response.of(200, "<form method=\"post\" action=\"/transfer\">" + hidden
                + "<input name=\"to\"><input name=\"amount\"><button>Send</button></form>");
    }

    private Response transfer(Request r) {
        if ("GET".equals(r.method()) && !protection.getTransfers()) {
            return Response.of(405, "a transfer needs POST");
        }
        if (!"GET".equals(r.method()) && !"POST".equals(r.method())) {
            return Response.of(405, "a transfer needs POST");
        }
        Optional<Session> session = session(r);
        if (session.isEmpty()) {
            return Response.of(401, "not logged in");
        }
        if (protection.fetchChecks()) {
            Optional<String> refused = checkWhereItCameFrom(r);
            if (refused.isPresent()) {
                return Response.of(403, refused.get());
            }
        }
        if (protection.tokens() && "POST".equals(r.method())) {
            String sent = r.form().get("csrf");
            if (sent == null) {
                return Response.of(403, "no form token");
            }
            if (!sameSecret(sent, session.get().token())) {
                return Response.of(403, "the form token does not belong to this session");
            }
        }
        return move(session.get().user(), r.form().get("to"), r.form().get("amount"));
    }

    /** Fetch Metadata first; for browsers that do not send it, the Origin header. With neither, only the token is left. */
    private static Optional<String> checkWhereItCameFrom(Request r) {
        String site = r.header("sec-fetch-site");
        if (site != null) {
            return site.equals("same-origin") || site.equals("none") ? Optional.empty()
                    : Optional.of("Sec-Fetch-Site is " + site + ": a state-changing request must come from the bank's own pages");
        }
        String origin = r.header("origin");
        if (origin != null && !origin.equals(ORIGIN)) {
            return Optional.of("Origin is " + origin + ", not the bank");
        }
        return Optional.empty();
    }

    private Response apiTransfer(Request r) {
        String auth = r.header("authorization");
        String user = auth != null && auth.startsWith("Bearer ") ? apiTokens.get(auth.substring(7)) : null;
        if (user == null) {
            return Response.of(401, "no API token");
        }
        return move(user, r.form().get("to"), r.form().get("amount"));
    }

    private Response move(String from, String to, String amountText) {
        int amount;
        try {
            amount = Integer.parseInt(amountText == null ? "" : amountText);
        } catch (NumberFormatException e) {
            return Response.of(400, "amount is not a number");
        }
        if (to == null || !balances.containsKey(to) || to.equals(from) || amount <= 0 || amount > balances.get(from)) {
            return Response.of(400, "transfer refused");
        }
        balances.merge(from, -amount, Integer::sum);
        balances.merge(to, amount, Integer::sum);
        transfers++;
        return Response.of(200, "sent " + amount + " to " + to);
    }
}
