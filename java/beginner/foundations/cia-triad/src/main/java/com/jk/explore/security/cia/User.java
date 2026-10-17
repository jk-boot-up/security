package com.jk.explore.security.cia;

/** A person using the store's order system, with the role they were given. */
public record User(String name, Role role) {
}
