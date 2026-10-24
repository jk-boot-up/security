package com.jk.explore.security.depconf;

import java.util.List;
import java.util.function.Predicate;

/**
 * The fix for the names: each repository declares which package names it may supply. Names that belong to the online store are looked up in the online store's
 * repository <em>only</em>, and never fall through to the public one; everything else is looked up in the public one. (Gradle calls this exclusive content.)
 */
public final class ContentFilteringResolver implements Resolver {

    private final Predicate<String> internalName;
    private final Resolver inner = new FirstRepositoryResolver();

    public ContentFilteringResolver(Predicate<String> internalName) {
        this.internalName = internalName;
    }

    @Override
    public Resolved resolve(String name, VersionRange range, List<Repository> repositories) {
        boolean internal = internalName.test(name);
        List<Repository> allowed = repositories.stream().filter(r -> internal != r.isPublic()).toList();
        try {
            return inner.resolve(name, range, allowed);
        } catch (ResolutionException e) {
            throw new ResolutionException(name + (internal ? " is an internal name; the private repository has no " : " is not an internal name; the public repository has no ") + range);
        }
    }
}
