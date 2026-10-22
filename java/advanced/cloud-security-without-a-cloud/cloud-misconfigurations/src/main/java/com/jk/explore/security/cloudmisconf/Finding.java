package com.jk.explore.security.cloudmisconf;

/** One misconfiguration: a rule, how bad it is, the resource, and why it matters. */
public record Finding(String rule, String severity, String resource, String why) {

    @Override
    public String toString() {
        return String.format("%-9s %-22s %-28s %s", severity, rule, resource, why);
    }
}
