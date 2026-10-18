package com.jk.explore.security.asvs;

/**
 * One security requirement as the team writes it down: its own id, the chapter of the standard it comes from, the lowest level it applies
 * to, a short statement in the team's own words, how it is verified, the check that verifies it (empty for manual ones), and the edition of
 * the standard it was mapped against.
 */
record Requirement(String id, String chapter, Level level, Verification verification, String check, String edition, String statement) {

    Requirement {
        if (id.isBlank() || chapter.isBlank() || statement.isBlank() || edition.isBlank()) {
            throw new IllegalArgumentException("a requirement needs an id, a chapter, an edition and a statement");
        }
        if ((verification == Verification.MANUAL) != check.isEmpty()) {
            throw new IllegalArgumentException(id + ": automated and configuration requirements name a check, manual ones do not");
        }
    }
}
