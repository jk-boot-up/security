package com.jk.explore.security.depconf;

import java.util.Comparator;
import java.util.List;

/** Asks every repository and takes the highest version in range: how pip with {@code --extra-index-url} and several other tools behave. */
public final class HighestVersionResolver implements Resolver {

    @Override
    public Resolved resolve(String name, VersionRange range, List<Repository> repositories) {
        return repositories.stream()
                .flatMap(r -> r.find(name, range).stream().map(a -> new Resolved(a, r)))
                .max(Comparator.comparing((Resolved x) -> x.artifact().version()))
                .orElseThrow(() -> new ResolutionException("no repository has " + name + " in " + range));
    }
}
