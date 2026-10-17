package com.jk.explore.security.accesscontrol;

/** A signed-in person: a name, a role, and the bearer token their requests carry. */
record User(String name, Role role, String token) {
}
