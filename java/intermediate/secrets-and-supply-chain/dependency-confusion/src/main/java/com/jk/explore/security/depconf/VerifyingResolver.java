package com.jk.explore.security.depconf;

import java.util.List;

/** Wraps another resolver and refuses anything whose version or digest differs from the lockfile, or that is not in it (dependency verification). */
public final class VerifyingResolver implements Resolver {

    private final Resolver inner;
    private final Lockfile lock;

    public VerifyingResolver(Resolver inner, Lockfile lock) {
        this.inner = inner;
        this.lock = lock;
    }

    @Override
    public Resolved resolve(String name, VersionRange range, List<Repository> repositories) {
        Lockfile.Entry expected = lock.get(name);
        if (expected == null) {
            throw new ResolutionException(name + " is not in the lockfile");
        }
        Resolved r = inner.resolve(name, range, repositories);
        if (!r.artifact().version().equals(expected.version()) || !r.artifact().digest().equals(expected.digest())) {
            throw new ResolutionException(name + " " + r.artifact().version() + " from " + r.source().name() + " does not match the lockfile");
        }
        return r;
    }
}
