package com.jk.explore.security.attacks;

/** The bank's answer to one login attempt: the outcome, and the message the person sees. */
public record Reply(Outcome outcome, String message) {
}
