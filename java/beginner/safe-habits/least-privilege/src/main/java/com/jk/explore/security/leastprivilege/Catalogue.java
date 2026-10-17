package com.jk.explore.security.leastprivilege;

import java.util.List;
import java.util.Map;

/** Everything that can be done in the online store, and the roles that bundle some of it. */
final class Catalogue {

    static final List<String> PERMISSIONS = List.of(
            "orders:read", "orders:write", "orders:delete",
            "customers:read", "customers:write", "customers:delete",
            "payments:read", "payments:write",
            "refunds:create", "refunds:approve",
            "inventory:read", "inventory:write",
            "reports:read",
            "secrets:read", "secrets:write");

    static final Map<String, Policy> ROLES = Map.of(
            "admin", Policy.of("allow *"),
            "support", Policy.of("allow orders:read", "allow customers:read", "allow refunds:create"),
            "warehouse", Policy.of("allow inventory:read", "allow inventory:write", "allow orders:read"),
            "finance", Policy.of("allow payments:read", "allow refunds:approve", "allow reports:read"),
            "exporter", Policy.of("allow orders:read", "allow reports:read"));

    private Catalogue() {
    }
}
