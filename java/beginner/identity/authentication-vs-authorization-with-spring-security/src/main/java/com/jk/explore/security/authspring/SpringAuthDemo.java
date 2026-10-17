package com.jk.explore.security.authspring;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpMethod;

/** Runs the online store story in five numbered acts, with Spring Security, and prints exact, repeatable results. */
public class SpringAuthDemo {

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        Logger.getLogger("").setLevel(Level.WARNING);   // keep Spring's start-up chatter out of the output
        StringBuilder out = new StringBuilder();
        act1NoChecks(out);
        act2Authentication(out);
        act3LoginIsNotEnough(out);
        act4Authorization(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1NoChecks(StringBuilder out) {
        line(out, "ONE. Spring Security present, but every request permitted.");
        try (Shop shop = new Shop(OpenConfig.class)) {
            line(out, "  nobody refunds O-1: " + shop.send(HttpMethod.POST, "/orders/O-1/refund", null, null)
                    + "  <- NOBODY ASKED WHO");
            line(out, "  nobody views O-1: " + shop.send(HttpMethod.GET, "/orders/O-1", null, null));
        }
    }

    private static void act2Authentication(StringBuilder out) {
        line(out, "TWO. Authentication: HTTP Basic, passwords stored as BCrypt.");
        try (Shop shop = new Shop(LoginOnlyConfig.class)) {
            line(out, "  a stored password starts with " + shop.encoder().encode("x").substring(0, 7) + " (BCrypt, cost 4)");
            line(out, "  request with no credentials: " + shop.send(HttpMethod.GET, "/orders/O-2", null, null));
            line(out, "  ben with a wrong password: " + shop.send(HttpMethod.GET, "/orders/O-2", "ben", "guess"));
            line(out, "  ben with his password: " + shop.as("ben", HttpMethod.GET, "/orders/O-2"));
        }
    }

    private static void act3LoginIsNotEnough(StringBuilder out) {
        line(out, "THREE. Authenticated is not the same as authorized.");
        try (Shop shop = new Shop(LoginOnlyConfig.class)) {
            line(out, "  ben, a customer, refunds O-1: " + shop.as("ben", HttpMethod.POST, "/orders/O-1/refund")
                    + "  <- LOGGED IN, SO ALLOWED");
            line(out, "  ben views ana's order O-1: " + shop.as("ben", HttpMethod.GET, "/orders/O-1")
                    + "  <- NOT HIS ORDER");
        }
    }

    private static void act4Authorization(StringBuilder out) {
        line(out, "FOUR. Authorization: @PreAuthorize rules on the endpoints.");
        try (Shop shop = new Shop(PolicyConfig.class)) {
            line(out, "  no credentials refunds O-1: " + shop.send(HttpMethod.POST, "/orders/O-1/refund", null, null));
            line(out, "  ben refunds O-1: " + shop.as("ben", HttpMethod.POST, "/orders/O-1/refund"));
            line(out, "  ben views ana's order O-1: " + shop.as("ben", HttpMethod.GET, "/orders/O-1"));
            line(out, "  ben views his own order O-2: " + shop.as("ben", HttpMethod.GET, "/orders/O-2"));
            line(out, "  sam, from support, views O-1: " + shop.as("sam", HttpMethod.GET, "/orders/O-1"));
            line(out, "  sam refunds O-1: " + shop.as("sam", HttpMethod.POST, "/orders/O-1/refund"));
            line(out, "  ana cancels ben's order O-2: " + shop.as("ana", HttpMethod.POST, "/orders/O-2/cancel"));
            line(out, "  alex, an admin, cancels O-2: " + shop.as("alex", HttpMethod.POST, "/orders/O-2/cancel"));
        }
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: a forgotten rule, and a password check on every request.");
        try (Shop shop = new Shop(PolicyConfig.class)) {
            int before = shop.encoder().checks();
            shop.as("sam", HttpMethod.GET, "/orders/O-1");
            shop.as("sam", HttpMethod.GET, "/orders/O-2");
            shop.as("sam", HttpMethod.POST, "/orders/O-1/refund");
            line(out, "  sam makes 3 requests with HTTP Basic: " + (shop.encoder().checks() - before) + " password checks");
            line(out, "  ben exports every order, the endpoint has no @PreAuthorize: "
                    + shop.as("ben", HttpMethod.GET, "/orders/export") + "  <- FORGOTTEN RULE");
        }
        try (Shop shop = new Shop(LayeredConfig.class)) {
            line(out, "  with a rule on the address too, ben exports: " + shop.as("ben", HttpMethod.GET, "/orders/export"));
            line(out, "  alex exports: " + shop.as("alex", HttpMethod.GET, "/orders/export"));
        }
    }
}
