package com.jk.explore.security.threatmodel;

/** A candidate threat found by a rule: a STRIDE category against one element or flow. The key, such as T:F2, is stable across runs. */
record Threat(Stride stride, String target, String description) {

    String key() {
        return stride.letter + ":" + target;
    }
}
