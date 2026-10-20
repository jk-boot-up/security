package com.jk.explore.security.container;

/** Whether an attack works against a container, and the setting that decides it. */
public record Outcome(Attack attack, boolean possible, String reason) {

    @Override
    public String toString() {
        return String.format("%-22s %-9s %s", attack.id(), possible ? "POSSIBLE" : "blocked", reason);
    }
}
