package com.jk.explore.security.linuxhard;

/**
 * One thing the checker found. {@code fixable} is true when the fix is a safe, mechanical change of a permission (the checker can apply it);
 * accounts, services and settings need a person to decide. The report line shows the rule and subject; {@code auto} marks a fixable one.
 */
public record Finding(String rule, Severity severity, String subject, String message, boolean fixable) {

    @Override
    public String toString() {
        return String.format("%-8s %-19s %s%s", severity, rule, subject, fixable ? "  auto" : "");
    }
}
