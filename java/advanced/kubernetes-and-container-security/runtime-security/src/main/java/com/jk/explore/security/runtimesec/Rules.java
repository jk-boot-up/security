package com.jk.explore.security.runtimesec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The three tests of the bank's runtime rules, the same ones as rules/bank-runtime-rules.yaml. */
public final class Rules {

    public static final String SHELL = "Shell in container";
    public static final String WRITE = "Unexpected write in container";
    public static final String CONNECT = "Unexpected outbound connection";

    static final Set<String> SHELLS = Set.of("sh", "bash", "ash", "dash", "zsh");

    /**
     * What the service is expected to do.
     *
     * @param writablePrefixes folders it writes to
     * @param allowedIps addresses it connects to
     */
    public record Baseline(List<String> writablePrefixes, Set<String> allowedIps) {
    }

    private Rules() {
    }

    /** The alerts for the events of known containers (container id to pod name), once for each rule, pod and detail. */
    public static Set<Alert> evaluate(List<Event> events, Map<String, String> podByContainer, Baseline baseline) {
        Set<Alert> alerts = new LinkedHashSet<>();
        for (Event e : events) {
            String pod = e.containerId() == null ? null : podByContainer.get(e.containerId());
            if (pod == null || e.key() == null) {
                continue;
            }
            switch (e.key()) {
                case "exec" -> {
                    if (e.success() && SHELLS.contains(e.comm())) {
                        alerts.add(new Alert(SHELL, pod, e.comm()));
                    }
                }
                case "write" -> {
                    for (String path : e.paths()) {
                        if (baseline.writablePrefixes().stream().noneMatch(path::startsWith)) {
                            alerts.add(new Alert(WRITE, pod, path + (e.success() ? "" : " (refused)")));
                        }
                    }
                }
                case "connect" -> {
                    if (e.address() != null && !e.ip().startsWith("127.") && !baseline.allowedIps().contains(e.ip())) {
                        alerts.add(new Alert(CONNECT, pod, e.address()));
                    }
                }
                default -> {
                }
            }
        }
        return alerts;
    }
}
