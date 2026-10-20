package com.jk.explore.security.depconf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A CI build: it resolves each dependency, installs it, and runs the package's install script, which has the build machine's secrets in its environment. */
public final class Build {

    /** What happened in the build. */
    public record Report(List<Resolved> installed, List<String> scriptsRun, List<String> errors) {
    }

    private final Resolver resolver;
    private final List<Repository> repositories;

    public Build(Resolver resolver, List<Repository> repositories) {
        this.resolver = resolver;
        this.repositories = repositories;
    }

    public Report run(Map<String, VersionRange> dependencies) {
        List<Resolved> installed = new ArrayList<>();
        List<String> scripts = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (var d : new LinkedHashMap<>(dependencies).entrySet()) {
            try {
                Resolved r = resolver.resolve(d.getKey(), d.getValue(), repositories);
                installed.add(r);
                if (r.artifact().installScript() != null) {
                    scripts.add(r.artifact().name() + ": " + r.artifact().installScript());
                }
            } catch (ResolutionException e) {
                errors.add(e.getMessage());
            }
        }
        return new Report(installed, scripts, errors);
    }
}
