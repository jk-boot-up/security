package com.jk.explore.security.cors;

import java.util.ArrayList;
import java.util.List;

/**
 * The bank's API on https://bank.example. {@code /account} needs the session cookie and returns a balance, {@code /rates} is public, and
 * {@code /transfer} changes money. It records every request it actually processes, so the demo can show what CORS does not stop.
 */
public final class BankApi {

    public static final String ORIGIN = "https://bank.example";

    private final CorsPolicy policy;
    private final boolean varyOrigin;
    private final List<String> processed = new ArrayList<>();

    public BankApi(CorsPolicy policy, boolean varyOrigin) {
        this.policy = policy;
        this.varyOrigin = varyOrigin;
    }

    public List<String> processed() {
        return processed;
    }

    public Response handle(Request r) {
        String origin = r.header("origin");
        if (r.method().equals("OPTIONS")) {
            return preflight(r, origin); // a preflight is answered before any application code runs, and carries no cookie
        }
        processed.add(r.method() + " " + r.path());
        Response response = switch (r.path()) {
            case "/rates" -> Response.of(200, "rates: 4.1%").with("access-control-allow-origin", "*");
            case "/account" -> r.hasSessionCookie() ? Response.of(200, "balance: 125000 cents") : Response.of(401, "not logged in");
            case "/transfer" -> r.hasSessionCookie() ? Response.of(200, "sent 500 to mallory") : Response.of(401, "not logged in");
            default -> Response.of(404, "not found");
        };
        return cors(response, origin, r.path());
    }

    private Response cors(Response response, String origin, String path) {
        if (path.equals("/rates")) {
            return response; // public data: the wildcard is fine, and credentials are never involved
        }
        Response out = response;
        if (varyOrigin) {
            out = out.with("vary", "Origin"); // the answer depends on the Origin header, so caches must keep one copy for each
        }
        return policy.allow(origin).map(o -> withAllow(response, o)).map(x -> varyOrigin ? x.with("vary", "Origin") : x).orElse(out);
    }

    private Response preflight(Request r, String origin) {
        Response base = Response.of(204, "");
        return policy.allow(origin).map(o -> withAllow(base, o)
                .with("access-control-allow-methods", "GET, POST, PUT")
                .with("access-control-allow-headers", "content-type, x-requested-with")
                .with("access-control-max-age", "600")).orElse(base);
    }

    private static Response withAllow(Response r, String origin) {
        return r.with("access-control-allow-origin", origin).with("access-control-allow-credentials", "true");
    }
}
