package com.jk.explore.security.leaks;

import java.sql.SQLException;
import java.util.Map;

/** The shop's checkout call. {@code fields} are the request's parameters: name, card, cvv, token and so on. */
public final class Checkout {

    /** What the customer's browser receives. */
    public record Response(int status, String body) {
    }

    private final Logger log;
    private final boolean safe;
    private int references;

    private Checkout(Logger log, boolean safe) {
        this.log = log;
        this.safe = safe;
    }

    public static Checkout naive(Logger log) {
        return new Checkout(log, false);
    }

    public static Checkout fixed(Logger log) {
        return new Checkout(log, true);
    }

    public Response pay(Map<String, String> fields, Secret sessionToken) {
        String name = fields.get("name");
        if (safe) {
            log.info("checkout started for " + name + " with token " + sessionToken); // a Secret prints as [REDACTED]
        } else {
            log.info("checkout started: " + fields + " token=" + sessionToken.reveal());
        }
        try {
            Database.saveOrder("o-" + fields.get("order"));
            return new Response(200, "paid");
        } catch (SQLException e) {
            if (safe) {
                String ref = String.format("ref-%04d", ++references);
                log.error("checkout failed [" + ref + "]", e);
                return new Response(500, "Something went wrong. Quote reference " + ref + " to support.");
            }
            log.error("checkout failed", e);
            return new Response(500, describe(e));
        }
    }

    /** What many frameworks do in development mode, and some leave on: the exception, its message and the call chain, as the page. */
    private static String describe(Throwable e) {
        StringBuilder b = new StringBuilder(e.getClass().getName()).append(": ").append(e.getMessage());
        for (StackTraceElement f : e.getStackTrace()) {
            if (f.getClassName().endsWith(".Database") || f.getClassName().endsWith(".Checkout")) { // the shop's own frames; the rest depends on who called
                b.append("\n\tat ").append(f.getClassName()).append('.').append(f.getMethodName());
            }
        }
        return b.toString();
    }
}
