package com.jk.explore.security.dast;

import java.util.List;

/** One kind of alert in a ZAP report: its name, its risk (3 high, 2 medium, 1 low, 0 informational) and the paths it was raised on. */
public record Alert(String name, int risk, List<String> paths) {

    boolean on(String path) {
        return paths.contains(path);
    }
}
