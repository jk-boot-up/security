package com.jk.explore.security.validation;

import java.util.HashSet;
import java.util.Set;

/** Registered login names. The naive registry compares raw text; the safe one compares canonical names. */
public final class Accounts {

    private final Set<String> naive = new HashSet<>();
    private final Set<Username> canonical = new HashSet<>();

    /** Registers by raw text, as a registry with no canonical form does. */
    public boolean registerNaively(String raw) {
        return naive.add(raw);
    }

    /** Registers by canonical name. Refuses a name that is invalid or that is another spelling of a taken name. */
    public void register(String raw) throws Rejected {
        Username name = Username.parse(raw);
        if (!canonical.add(name)) {
            throw new Rejected("username", "already taken");
        }
    }
}
