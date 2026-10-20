package com.jk.explore.security.container;

/** One thing an attacker with code execution inside the container tries. */
public record Attack(String id, String goal) {
}
