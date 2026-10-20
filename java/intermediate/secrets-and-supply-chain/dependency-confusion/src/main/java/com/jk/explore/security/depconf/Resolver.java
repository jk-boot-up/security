package com.jk.explore.security.depconf;

import java.util.List;

/** Chooses an artifact for a dependency from the configured repositories. The implementations are the different ways build tools decide. */
public interface Resolver {

    Resolved resolve(String name, VersionRange range, List<Repository> repositories);
}
