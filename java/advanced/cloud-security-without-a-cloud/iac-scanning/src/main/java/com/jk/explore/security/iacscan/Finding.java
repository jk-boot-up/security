package com.jk.explore.security.iacscan;

/** One rule that one container of one resource breaks. */
public record Finding(String rule, String resource, String container) {

    @Override
    public String toString() {
        return String.format("%-4s %-22s %s", rule, resource, container);
    }
}
