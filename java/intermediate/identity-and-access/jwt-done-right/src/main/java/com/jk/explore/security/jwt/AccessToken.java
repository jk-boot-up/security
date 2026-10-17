package com.jk.explore.security.jwt;

import java.util.Set;

/** A token that passed every check: who it is for, what it allows, and when it stops working. */
record AccessToken(String subject, Set<String> scopes, long expiresAt) {
}
