package com.jk.explore.security.dragonmodel;

import java.util.List;

/** A node of an attack tree: either a leaf (one step an attacker takes) or a gate (OR: any child will do; AND: every child is needed). */
public sealed interface Node permits Node.Leaf, Node.Gate {

    String name();

    /**
     * One step. {@code cost} is what it costs the attacker in euros, {@code probability} the chance that it works against the system <em>as it is now</em>,
     * and {@code abuseCase} the id of the executable abuse case that checks it (empty if none).
     */
    record Leaf(String id, String name, double cost, double probability, String abuseCase) implements Node {

        public Leaf withEffect(double costFactor, double probabilityFactor) {
            return new Leaf(id, name, cost * costFactor, Math.min(1.0, probability * probabilityFactor), abuseCase);
        }
    }

    enum Kind { AND, OR }

    record Gate(String name, Kind kind, List<Node> children) implements Node {
    }

    static Node or(String name, Node... children) {
        return new Gate(name, Kind.OR, List.of(children));
    }

    static Node and(String name, Node... children) {
        return new Gate(name, Kind.AND, List.of(children));
    }

    static Leaf leaf(String id, String name, double cost, double probability, String abuseCase) {
        return new Leaf(id, name, cost, probability, abuseCase);
    }
}
