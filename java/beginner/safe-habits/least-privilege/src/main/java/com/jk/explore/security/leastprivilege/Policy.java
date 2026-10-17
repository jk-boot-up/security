package com.jk.explore.security.leastprivilege;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of allow and deny statements about permissions written as "resource:action", where either part may be "*".
 * Everything not allowed is denied (deny by default), and an explicit deny beats any allow.
 */
record Policy(List<Statement> statements) {

    enum Decision { ALLOW, DENY_EXPLICIT, DENY_DEFAULT }

    record Statement(boolean allow, String pattern) {
    }

    /** Statements are written "allow orders:read" or "deny secrets:*". */
    static Policy of(String... lines) {
        List<Statement> statements = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split(" ", 2);
            if (parts.length != 2 || !(parts[0].equals("allow") || parts[0].equals("deny"))) {
                throw new IllegalArgumentException("not a statement: " + line);
            }
            statements.add(new Statement(parts[0].equals("allow"), parts[1]));
        }
        return new Policy(List.copyOf(statements));
    }

    static Policy combine(List<Policy> policies) {
        List<Statement> all = new ArrayList<>();
        policies.forEach(p -> all.addAll(p.statements()));
        return new Policy(List.copyOf(all));
    }

    static boolean matches(String pattern, String permission) {
        if (pattern.equals("*")) {
            return true;
        }
        String[] want = pattern.split(":", 2);
        String[] have = permission.split(":", 2);
        if (want.length != 2 || have.length != 2) {
            return false;
        }
        return (want[0].equals("*") || want[0].equals(have[0])) && (want[1].equals("*") || want[1].equals(have[1]));
    }

    Decision decide(String permission) {
        boolean allowed = false;
        for (Statement statement : statements) {
            if (matches(statement.pattern(), permission)) {
                if (!statement.allow()) {
                    return Decision.DENY_EXPLICIT;
                }
                allowed = true;
            }
        }
        return allowed ? Decision.ALLOW : Decision.DENY_DEFAULT;
    }

    boolean allows(String permission) {
        return decide(permission) == Decision.ALLOW;
    }

    /** The permissions of the catalogue this policy allows: the blast radius if the credential holding it is stolen. */
    List<String> reachable(List<String> catalogue) {
        return catalogue.stream().filter(this::allows).toList();
    }
}
