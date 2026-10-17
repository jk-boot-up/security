package com.jk.explore.security.threatmodel;

/** One arrow in the data-flow diagram: what moves from where to where, and how it is protected on the way. */
record Flow(String id, String from, String to, String data, boolean encrypted, boolean authenticated, boolean money) {
}
