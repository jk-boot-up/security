package com.jk.explore.security.depconf;

import java.util.List;
import java.util.Set;

/** Detection: for each internal package name, does a public repository also have it? If so, somebody registered your name. */
public final class NameWatch {

    private NameWatch() {
    }

    public static List<String> shadowedNames(Set<String> internalNames, Repository publicRepository) {
        return internalNames.stream().sorted().filter(n -> !publicRepository.find(n, VersionRange.any()).isEmpty()).toList();
    }
}
