package com.jk.explore.security.cia;

/** One request arriving at the order server, from a named client. */
public record Request(String client) {
}
