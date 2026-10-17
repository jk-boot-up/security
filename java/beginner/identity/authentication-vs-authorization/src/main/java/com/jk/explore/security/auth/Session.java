package com.jk.explore.security.auth;

/** Proof of a successful sign-in: a token, who it belongs to, the role they had then, and when it was issued. */
public record Session(String token, String username, Role roleAtLogin, int issuedAt) {
}
