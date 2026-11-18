package com.jk.explore.security.iacscan;

import java.util.List;
import java.util.regex.Pattern;

/** The store's eight rules for a container. */
public final class Rules {

    private static final Pattern SECRETISH = Pattern.compile("(?i).*(PASSWORD|SECRET|TOKEN|API_?KEY).*");

    private Rules() {
    }

    /**
     * The tag of an image name: what follows the last ':' or '@' in its last part, after the last '/'. Empty when
     * there is none, and then Kubernetes pulls "latest".
     */
    static String tag(String image) {
        String last = image.substring(image.lastIndexOf('/') + 1);
        int at = Math.max(last.lastIndexOf(':'), last.lastIndexOf('@'));
        return at < 0 ? "" : last.substring(at + 1);
    }

    public static final List<Rule> ALL = List.of(
            new Rule("K1", "privileged container", c -> c.security().path("privileged").asBoolean(false),
                    "securityContext: {privileged: true}", "securityContext: {privileged: false}"),
            new Rule("K2", "may run as root", c -> !c.security().path("runAsNonRoot").asBoolean(false) && !c.pod().path("securityContext").path("runAsNonRoot").asBoolean(false),
                    "imagePullPolicy: Always", "securityContext: {runAsNonRoot: true}"),
            new Rule("K3", "image is latest or has no tag", c -> {
                String tag = tag(c.container().path("image").asString(""));
                return tag.isEmpty() || tag.equals("latest");
            }, "image: shop/orders", "image: shop/web:1.8.2"),
            new Rule("K4", "no cpu and memory limits", c -> !c.container().path("resources").path("limits").has("memory") || !c.container().path("resources").path("limits").has("cpu"),
                    "imagePullPolicy: Always", "resources: {limits: {cpu: 1, memory: 1Gi}}"),
            new Rule("K5", "secret written in the manifest", c -> {
                for (var e : c.container().path("env")) {
                    if (SECRETISH.matcher(e.path("name").asString("")).matches() && e.has("value")) {
                        return true;
                    }
                }
                return false;
            }, "env: [{name: DB_PASSWORD, value: x}]", "env: [{name: DB_PASSWORD, valueFrom: {secretKeyRef: {name: s, key: k}}}]"),
            new Rule("K6", "uses the host's network", c -> c.pod().path("hostNetwork").asBoolean(false), "pod: hostNetwork: true", "pod: hostNetwork: false"),
            new Rule("K7", "root filesystem is writable", c -> !c.security().path("readOnlyRootFilesystem").asBoolean(false), "imagePullPolicy: Always", "securityContext: {readOnlyRootFilesystem: true}"),
            new Rule("K8", "may gain more privileges", c -> !c.security().has("allowPrivilegeEscalation") || c.security().path("allowPrivilegeEscalation").asBoolean(true),
                    "imagePullPolicy: Always", "securityContext: {allowPrivilegeEscalation: false}"));
}
