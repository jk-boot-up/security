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

    /** The only repositories that may supply this name: the private ones for an internal name, the public ones otherwise. */
    public List<Repository> allowedFor(String name, List<Repository> repositories) {
        boolean internal = internalName.test(name);
        return repositories.stream().filter(r -> internal != r.isPublic()).toList();
    }

    public boolean isInternal(String name) {
        return internalName.test(name);
    }

    @Override
    public Resolved resolve(String name, VersionRange range, List<Repository> repositories) {
        boolean internal = internalName.test(name);
        try {
            return inner.resolve(name, range, allowedFor(name, repositories));
        } catch (ResolutionException e) {
            throw new ResolutionException(name + (internal ? " is an internal name; the private repository has no " : " is not an internal name; the public repository has no ") + range);
        }
    }
}
