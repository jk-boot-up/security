package com.jk.explore.security.provenance;

import java.util.Map;

/** The source a build starts from: where it lives, which branch, which commit, and the files at that commit. */
record Source(String repository, String ref, String commit, Map<String, byte[]> files) {
}
