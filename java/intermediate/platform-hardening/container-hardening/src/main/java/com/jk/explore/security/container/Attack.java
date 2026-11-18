package com.jk.explore.security.container;

/** One thing an attacker with code execution inside the container tries, and what it reaches: the container, the kernel, the host or the node's resources. */
public record Attack(String id, String goal, String reach) {
}
