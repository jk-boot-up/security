package com.jk.explore.security.attacktree;

import java.util.Set;
import java.util.TreeSet;

/** The properties of the bank system that the abuse cases test. Each field is switched on by one defence. */
public record BankConfig(Set<String> enabled) {

    public static BankConfig weak() {
        return new BankConfig(Set.of());
    }

    public BankConfig with(String change) {
        Set<String> s = new TreeSet<>(enabled);
        s.add(change);
        return new BankConfig(Set.copyOf(s));
    }

    public boolean has(String change) {
        return enabled.contains(change);
    }
}
