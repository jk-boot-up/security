package com.jk.explore.security.patchingtrivy;

/** One Maven library: group, artifact and exact version. Its package URL (purl) is how scanners name it. */
record Dependency(String group, String artifact, String version) {

    String purl() {
        return "pkg:maven/" + group + "/" + artifact + "@" + version;
    }

    /** The name Trivy and Gradle use: group:artifact. */
    String name() {
        return group + ":" + artifact;
    }

    String coordinates() {
        return name() + ":" + version;
    }
}
