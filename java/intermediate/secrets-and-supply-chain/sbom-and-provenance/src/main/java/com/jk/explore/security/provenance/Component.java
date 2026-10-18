package com.jk.explore.security.provenance;

/** One library the store is built with: its Maven coordinates and the bytes of its jar. */
record Component(String group, String name, String version, byte[] jar) {

    /** The package URL, the standard name for a package across ecosystems. */
    String purl() {
        return "pkg:maven/" + group + "/" + name + "@" + version;
    }

    String sha256() {
        return Digests.hex(Digests.sha256(jar));
    }
}
