package com.jk.explore.security.beanvalidation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/** Validation groups: the cheap checks first, then the ones that cost something, and each stage only if the one before found nothing wrong. */
public final class Basic {

    private Basic() {
    }

    /** Limits on sizes, which must hold before anything looks inside a value. */
    public interface Limits {
    }

    /** Checks that cost something: a lookup, a slow pattern. */
    public interface Expensive {
    }

    /** The order: limits, then everything not in a group (Default), then the expensive checks. */
    @GroupSequence({Limits.class, Default.class, Expensive.class})
    public interface Ordered {
    }
}
