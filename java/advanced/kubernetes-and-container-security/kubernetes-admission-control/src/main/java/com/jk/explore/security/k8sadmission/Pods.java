package com.jk.explore.security.k8sadmission;

/** Pod and Deployment manifests for the requests, built as text. */
public final class Pods {

    private Pods() {
    }

    /** What a request looks like. */
    public record Spec(String name, String image, boolean privileged, boolean limits, boolean readOnlyRootFilesystem) {

        public Spec withName(String n) {
            return new Spec(n, image, privileged, limits, readOnlyRootFilesystem);
        }
    }

    public static Spec ok(String image) {
        return new Spec("payments", image, false, true, true);
    }

    static String container(Spec s, String indent) {
        StringBuilder b = new StringBuilder();
        b.append(indent).append("- name: app\n").append(indent).append("  image: ").append(s.image()).append('\n');
        if (s.limits()) {
            b.append(indent).append("  resources: {limits: {cpu: 500m, memory: 256Mi}}\n");
        }
        if (s.privileged() || s.readOnlyRootFilesystem()) {
            b.append(indent).append("  securityContext: {");
            b.append(s.privileged() ? "privileged: true" : "");
            if (s.readOnlyRootFilesystem()) {
                b.append(s.privileged() ? ", " : "").append("readOnlyRootFilesystem: true");
            }
            b.append("}\n");
        }
        return b.toString();
    }

    public static String pod(Spec s) {
        return "apiVersion: v1\nkind: Pod\nmetadata: {name: " + s.name() + "}\nspec:\n  containers:\n" + container(s, "    ");
    }

    public static String deployment(Spec s) {
        return "apiVersion: apps/v1\nkind: Deployment\nmetadata: {name: " + s.name() + "}\nspec:\n  replicas: 1\n  selector:\n    matchLabels: {app: " + s.name() + "}\n  template:\n    metadata:\n      labels: {app: " + s.name()
                + "}\n    spec:\n      containers:\n" + container(s, "        ");
    }
}
