package com.jk.explore.security.hashing;

import java.util.Map;

/** What an attacker got from a stolen table: which passwords were found, and how many guesses it took. */
public record Cracked(Map<String, String> found, int guesses) {
}
