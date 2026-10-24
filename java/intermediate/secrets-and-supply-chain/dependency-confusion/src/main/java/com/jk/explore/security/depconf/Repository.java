package com.jk.explore.security.depconf;

import java.util.ArrayList;
import java.util.List;

/** A place packages are published to. Anyone can publish to a public repository; only the online store can publish to its private one. */
public final class Repository {

    private final String name;
    private final boolean isPublic;
    private final List<Artifact> artifacts = new ArrayList<>();

    public Repository(String name, boolean isPublic) {
        this.name = name;
        this.isPublic = isPublic;
    }

    public String name() {
        return name;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public Repository publish(Artifact a) {
        artifacts.add(a);
        return this;
    }

    public List<Artifact> find(String packageName, VersionRange range) {
        return artifacts.stream().filter(a -> a.name().equals(packageName) && range.contains(a.version())).toList();
    }
}
