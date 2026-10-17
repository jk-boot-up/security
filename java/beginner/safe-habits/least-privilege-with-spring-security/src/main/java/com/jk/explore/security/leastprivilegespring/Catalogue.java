package com.jk.explore.security.leastprivilegespring;

import java.util.List;
import java.util.Map;

/** Everything that can be done in the online store, and who holds what. Roles name a job; authorities name one action. */
final class Catalogue {

    static final String PASSWORD = "demo-password";

    static final List<String> PERMISSIONS = List.of(
            "orders:read", "orders:write", "orders:delete",
            "customers:read", "customers:write", "customers:delete",
            "payments:read", "payments:write",
            "refunds:create", "refunds:approve",
            "inventory:read", "inventory:write",
            "reports:read",
            "secrets:read", "secrets:write");

    /** A person or service account: a role and the exact actions it was given. */
    record Account(String name, String role, List<String> permissions) {
    }

    static final Map<String, Account> ACCOUNTS = Map.of(
            "alice", new Account("alice", "SUPPORT", List.of("orders:read", "customers:read", "refunds:create")),
            "bob", new Account("bob", "WAREHOUSE", List.of("inventory:read", "inventory:write", "orders:read")),
            "carol", new Account("carol", "FINANCE", List.of("payments:read", "refunds:approve", "reports:read")),
            "dave", new Account("dave", "ADMIN", PERMISSIONS),
            "exporter", new Account("exporter", "EXPORTER", List.of("orders:read", "reports:read")));

    private Catalogue() {
    }
}
