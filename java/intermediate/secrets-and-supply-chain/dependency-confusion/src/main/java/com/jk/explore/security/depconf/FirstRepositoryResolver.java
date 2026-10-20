package com.jk.explore.security.depconf;

import java.util.Comparator;
import java.util.List;

/** Goes through the repositories in order and takes the highest matching version from the first one that has the package: how Maven and Gradle behave by default. */
public final class FirstRepositoryResolver implements Resolver {

    @Override
    public Resolved resolve(String name, VersionRange range, List<Repository> repositories) {
        for (Repository r : repositories) {
            var found = r.find(name, range).stream().max(Comparator.comparing(Artifact::version));
            if (found.isPresent()) {
                return new Resolved(found.get(), r);
            }
        }
        throw new ResolutionException("no repository has " + name + " in " + range);
    }
}
