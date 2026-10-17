package com.jk.explore.security.auth;

/** A person who can sign in: a username and the role they were given. */
public record Account(String username, Role role) {
}
