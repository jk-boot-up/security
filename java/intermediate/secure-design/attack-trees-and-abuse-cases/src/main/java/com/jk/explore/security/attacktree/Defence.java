package com.jk.explore.security.attacktree;

import java.util.List;
import java.util.Map;

/**
 * A defence the bank could buy: what it costs, and what it does to the leaves of the tree (a factor for the attacker's cost and one for the chance of success;
 * a probability factor of 0 removes the step). {@code configChange} is the matching change to the bank system the abuse cases run against.
 */
public record Defence(String id, String name, int price, Map<String, double[]> effects, String configChange) {

    static double[] remove() {
        return new double[] {1, 0};
    }

    static double[] times(double cost, double probability) {
        return new double[] {cost, probability};
    }

    public static List<Defence> all() {
        return List.of(
                new Defence("OWN", "check that the account belongs to the signed-in user", 2_000, Map.of("B1", remove()), "ownership"),
                new Defence("CSRF", "form tokens and SameSite cookies", 3_000, Map.of("B2b", remove()), "csrf"),
                new Defence("BREACH", "refuse passwords that appear in leaks", 5_000, Map.of("A2b", times(1, 0.1), "A3b", times(1, 0.1)), "breached"),
                new Defence("OTP", "one-time codes from an app (not SMS)", 15_000, Map.of("A2b", remove(), "A3a", remove(), "A3b", remove()), "otp"),
                new Defence("PASSKEY", "passkeys for sign-in and payments", 40_000, Map.of("A1b", remove(), "A2b", remove(), "A3a", remove()), "passkey"),
                new Defence("CSP", "strict content security policy and HttpOnly cookies", 10_000, Map.of("A4a", times(3, 0.2)), "csp"),
                new Defence("PATCH", "update libraries within a week", 8_000, Map.of("D1", times(2, 0.2)), "patched"),
                new Defence("DUAL", "two people approve operational transfers", 12_000, Map.of("C", times(5, 0.2)), "dual"));
    }

    /** The tree after this defence is in place. */
    public Node applyTo(Node n) {
        return switch (n) {
            case Node.Leaf l -> effects.containsKey(l.id()) ? l.withEffect(effects.get(l.id())[0], effects.get(l.id())[1]) : l;
            case Node.Gate g -> new Node.Gate(g.name(), g.kind(), g.children().stream().map(this::applyTo).toList());
        };
    }
}
