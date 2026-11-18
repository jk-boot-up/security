package com.jk.explore.security.k8sadmission;

/**
 * A teaching class: the no-latest-tag policy's expression, redone in Java step by step.
 * The policy says: c.image.contains('@') || (c.image.lastIndexOf(':') > c.image.lastIndexOf('/') && !c.image.endsWith(':latest'))
 */
public record TagRule(String image, boolean digest, boolean tag, boolean latest) {

    public static TagRule read(String image) {
        boolean digest = image.contains("@");
        // a ':' before the last '/' is a port, not a tag
        boolean tag = image.lastIndexOf(':') > image.lastIndexOf('/');
        boolean latest = image.endsWith(":latest");
        return new TagRule(image, digest, tag, latest);
    }

    public boolean allowed() {
        return digest || (tag && !latest);
    }
}
