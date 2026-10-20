package com.jk.explore.security.depconf;

/** Which artifact a build picked, and from which repository. */
public record Resolved(Artifact artifact, Repository source) {

    @Override
    public String toString() {
        return artifact.name() + " " + artifact.version() + " from " + source.name();
    }
}
