package com.jk.explore.security.patching;

import java.util.List;

/** One entry of the inventory: a name, a version, and the references of the components it depends on. */
record Component(String name, String version, List<String> dependsOn) {

    String ref() {
        return name + "@" + version;
    }
}
