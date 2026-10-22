package com.jk.explore.security.dragonmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** For each abuse case: where in the bank's diagram it lands, what kind of threat it is in STRIDE terms, which defences answer it, and what it costs the attacker according to the tree. */
public final class Cases {

    /** The facts about one abuse case. */
    public record Entry(String id, String element, String stride, Set<String> answers, double cost, double chance) {

        /** High if it is cheap or likely, Medium if it costs up to 5,000 EUR, Low beyond that. */
        public String severity() {
            if (cost <= 500 || chance >= 0.5) {
                return "High";
            }
            return cost <= 5000 ? "Medium" : "Low";
        }

        public String score() {
            return String.format("cost %,.0f EUR, chance %.0f%%", cost, chance * 100);
        }
    }

    private static final Map<String, String[]> PLACE = Map.of(
            "AC1", new String[] {"signin", "Spoofing"},
            "AC2", new String[] {"signin", "Spoofing"},
            "AC3", new String[] {"sms", "Information disclosure"},
            "AC4", new String[] {"web", "Tampering"},
            "AC5", new String[] {"transfer", "Elevation of privilege"},
            "AC6", new String[] {"request", "Tampering"},
            "AC7", new String[] {"approval", "Elevation of privilege"},
            "AC8", new String[] {"web", "Elevation of privilege"});

    private static final Map<String, Set<String>> ANSWERS = Map.of(
            "AC1", Set.of("passkey"), "AC2", Set.of("otp", "passkey", "breached"), "AC3", Set.of("otp", "passkey"), "AC4", Set.of("csp"),
            "AC5", Set.of("ownership"), "AC6", Set.of("csrf"), "AC7", Set.of("dual"), "AC8", Set.of("patched"));

    private Cases() {
    }

    /** Cost (summed) and chance (multiplied) of the steps of the tree that one abuse case checks: the steps of one branch are all needed. */
    public static List<Entry> all(Node tree) {
        List<Entry> out = new ArrayList<>();
        for (AbuseCase c : AbuseCase.all()) {
            List<Node.Leaf> leaves = new ArrayList<>();
            collect(tree, c.id(), leaves);
            double cost = leaves.stream().mapToDouble(Node.Leaf::cost).sum();
            double chance = leaves.stream().mapToDouble(Node.Leaf::probability).reduce(1.0, (a, b) -> a * b);
            out.add(new Entry(c.id(), PLACE.get(c.id())[0], PLACE.get(c.id())[1], ANSWERS.get(c.id()), cost, chance));
        }
        return out;
    }

    private static void collect(Node n, String abuseCase, List<Node.Leaf> into) {
        switch (n) {
            case Node.Leaf l -> {
                if (l.abuseCase().equals(abuseCase)) {
                    into.add(l);
                }
            }
            case Node.Gate g -> g.children().forEach(c -> collect(c, abuseCase, into));
        }
    }
}
