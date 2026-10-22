package com.jk.explore.security.beanvalidation;

/** Only the invisible-character rule, apart from the login's other rules. */
public record Handle(@NoInvisible String value) {
}
