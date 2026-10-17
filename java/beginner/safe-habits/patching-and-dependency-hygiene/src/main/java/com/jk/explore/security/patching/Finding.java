package com.jk.explore.security.patching;

import java.util.List;

/** A known problem found in this program's inventory: which advisory, which component, how it got there, how bad it is, and whether the program reaches it. */
record Finding(Advisory advisory, String componentRef, List<String> path, double score, String severity, boolean reachable) {

    boolean direct() {
        return path.size() == 2;
    }

    String chain() {
        return String.join(" > ", path);
    }

    /** What to change. A transitive library is not named in the program's own build, so it is forced with a constraint. */
    String advice() {
        String name = advisory.component();
        if (direct()) {
            return "direct: change " + name + " from " + componentRef.substring(name.length() + 1) + " to " + advisory.fixed();
        }
        String parent = path.get(path.size() - 2);
        return "transitive: add a constraint forcing " + name + " to " + advisory.fixed() + ", then ask " + parent + " for a release that needs it";
    }

    String priority() {
        if (reachable && score >= 9.0) {
            return "fix now";
        }
        return reachable ? "fix inside the patch window" : "schedule with the next upgrade";
    }
}
