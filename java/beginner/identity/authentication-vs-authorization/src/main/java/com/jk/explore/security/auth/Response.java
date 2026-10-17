package com.jk.explore.security.auth;

/** What the shop sends back: a status and a short note on why. */
public record Response(Status status, String note) {
}
