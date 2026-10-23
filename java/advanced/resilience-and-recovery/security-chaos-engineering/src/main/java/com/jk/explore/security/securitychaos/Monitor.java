package com.jk.explore.security.securitychaos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Probes the health endpoint of each dependency; N failed probes in a row raise one alert. Latency is counted in probes, not seconds. */
final class Monitor {

    static final int THRESHOLD = 3;

    record Alert(String dependency, int failedProbes) {
    }

    private final Map<String, String> healthUrls;
    private final Http http;
    private final Map<String, Integer> failures = new LinkedHashMap<>();
    private final List<Alert> alerts = new ArrayList<>();

    Monitor(Map<String, String> healthUrls, Http http) {
        this.healthUrls = healthUrls;
        this.http = http;
    }

    /** One probe of every dependency. */
    void round() {
        healthUrls.forEach((name, url) -> {
            if (http.get(url).isPresent()) {
                failures.put(name, 0);
                return;
            }
            int n = failures.merge(name, 1, Integer::sum);
            if (n == THRESHOLD) {
                alerts.add(new Alert(name, n));
            }
        });
    }

    List<Alert> alerts() {
        return List.copyOf(alerts);
    }

    boolean alerted(String dependency) {
        return alerts.stream().anyMatch(a -> a.dependency().equals(dependency));
    }
}
