package com.jk.explore.security.containerhard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContainerHardeningDemo {

    /** What each attack that works means, in a few words. */
    static final Map<String, String> MEANING = Map.ofEntries(
            Map.entry("read-secrets", "secrets are in the environment, which every process of the container can read"),
            Map.entry("tamper-with-app", "the root file system is writable"),
            Map.entry("drop-and-run", "a file can be written and run from a writable directory"),
            Map.entry("become-root", "it runs as root"),
            Map.entry("dangerous-syscalls", "no seccomp filter: the dangerous system calls reach the kernel"),
            Map.entry("sniff-network", "root with the NET_RAW capability"),
            Map.entry("mount-host", "CAP_SYS_ADMIN: it can mount file systems"),
            Map.entry("docker-socket", "the Docker socket is mounted: control of every container, and root on the host"),
            Map.entry("host-files", "a host directory is mounted"),
            Map.entry("host-network", "it shares the host's network"),
            Map.entry("see-host-processes", "it shares the host's process list"),
            Map.entry("exhaust-memory", "no memory limit"),
            Map.entry("fork-bomb", "no process limit"));

    /** Why an attack that does not work is blocked. */
    static final Map<String, String> BLOCKED_BECAUSE = Map.ofEntries(
            Map.entry("dangerous-syscalls", "the seccomp profile denies them"),
            Map.entry("mount-host", "no CAP_SYS_ADMIN, and mount is denied by the profile"),
            Map.entry("docker-socket", "the Docker socket is not mounted"),
            Map.entry("host-files", "no host directories are mounted"),
            Map.entry("host-network", "its own network namespace"),
            Map.entry("see-host-processes", "its own process namespace"));

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The options of a stage; the seccomp profile is passed by file because the daemon of a lab may have none of its own. */
    static List<String> stage(String name, String seccomp) {
        List<String> o = new ArrayList<>();
        switch (name) {
            case "tutorial" -> o.addAll(List.of("--privileged", "--security-opt", "seccomp=unconfined", "--network", "host", "--pid", "host", "-v", "/var/run/docker.sock:/var/run/docker.sock", "-v", "/etc:/hostetc:ro"));
            case "unprivileged" -> o.addAll(List.of("--security-opt", "seccomp=unconfined"));
            default -> {
                o.addAll(List.of("--security-opt", "seccomp=" + seccomp));
                if (!name.equals("default")) {
                    o.addAll(List.of("--cap-drop", "ALL"));
                }
                if (List.of("non-root", "no-new-privileges", "read-only", "limits").contains(name)) {
                    o.addAll(List.of("--user", "10001"));
                }
                if (List.of("no-new-privileges", "read-only", "limits").contains(name)) {
                    o.addAll(List.of("--security-opt", "no-new-privileges"));
                }
                if (List.of("read-only", "limits").contains(name)) {
                    o.addAll(List.of("--read-only", "--tmpfs", "/tmp:rw,noexec,nosuid"));
                }
                if (name.equals("limits")) {
                    o.addAll(List.of("--memory", "512m", "--pids-limit", "200"));
                }
            }
        }
        return o;
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Image.build();
        String seccomp = Seccomp.profile().toString();
        act1(out, seccomp);
        act2(out, seccomp);
        act3(out, seccomp);
        act4(out, seccomp);
        act5(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String count(Map<String, Boolean> r) {
        return String.format("%2d of 13 attacks possible", Probes.possible(r));
    }

    private static void act1(StringBuilder out, String seccomp) throws IOException, InterruptedException {
        line(out, "ONE. What docker run with no options gives an attacker.");
        line(out, "  thirteen attempts, run inside a real container as the user it runs as: write a file, mount, read a limit, check the capabilities");
        line(out, "  (the lab's Docker starts containers with no seccomp filter at all, so the profile that denies what Docker's own default profile denies is passed by file)");
        Map<String, Boolean> r = Run.probe(stage("default", seccomp).subList(0, 2));
        for (String name : Probes.NAMES) {
            line(out, String.format("  %-20s %s  %s", name, r.get(name) ? "POSSIBLE" : "blocked ", r.get(name) ? MEANING.get(name) : BLOCKED_BECAUSE.get(name)));
        }
        line(out, "  " + Probes.possible(r) + " of 13 attacks possible");
    }

    private static void act2(StringBuilder out, String seccomp) throws IOException, InterruptedException {
        line(out, "TWO. The configuration from the tutorial.");
        line(out, "  docker run --privileged --security-opt seccomp=unconfined --network host --pid host -v /var/run/docker.sock:/var/run/docker.sock -v /etc:/hostetc:ro shop-app");
        Map<String, Boolean> r = Run.probe(stage("tutorial", seccomp));
        line(out, "  " + Probes.possible(r) + " of 13 attacks possible; blocked: " + Probes.NAMES.length + " - " + Probes.possible(r) + " = " + (Probes.NAMES.length - Probes.possible(r)));
        line(out, "  one command that works in a tutorial gives the process the host");
    }

    private static void act3(StringBuilder out, String seccomp) throws IOException, InterruptedException {
        line(out, "THREE. One setting at a time.");
        String[][] stages = {{"tutorial", "start: the tutorial configuration"}, {"unprivileged", "no privileged, no host network, PID or mounts"}, {"default", "a seccomp profile of the default kind"}, {"no-caps", "drop all capabilities"},
                {"non-root", "run as a user that is not root"}, {"no-new-privileges", "no new privileges"}, {"read-only", "read-only root, noexec temporary directory"}, {"limits", "memory and process limits"}};
        for (String[] st : stages) {
            Map<String, Boolean> r = Run.probe(stage(st[0], seccomp));
            line(out, String.format("  %-46s -> %s", st[1], count(r)));
        }
        line(out, "  each line adds one option to the one above; the number goes down only when the option takes away something an attack uses");
        line(out, "  no new privileges does not move the number here, because this image has no set-user-id program for an attacker to use; in an image that does, it is what stops it");
    }

    private static void act4(StringBuilder out, String seccomp) throws IOException, InterruptedException {
        line(out, "FOUR. The hardened container.");
        line(out, "  docker run --user 10001 --cap-drop=ALL --read-only --tmpfs /tmp:rw,noexec,nosuid --security-opt no-new-privileges --security-opt seccomp=<profile> --memory 512m --pids-limit 200 shop-app");
        Map<String, Boolean> r = Run.probe(stage("limits", seccomp));
        line(out, "  " + Probes.possible(r) + " of 13 attacks possible");
        Probes.possibleNames(r).forEach(n -> line(out, "    still possible: " + n + "  " + MEANING.get(n) + "; the application must read its secrets, so a hijacked application can too"));
        List<String> netRaw = new ArrayList<>(stage("limits", seccomp));
        netRaw.addAll(List.of("--cap-add", "NET_RAW"));
        line(out, "  hardened + NET_RAW, which looks harmless: " + count(Run.probe(netRaw)).strip() + " (NET_RAW matters only to root)");
        List<String> rootAdmin = new ArrayList<>(stage("limits", seccomp));
        rootAdmin.addAll(List.of("--user", "0", "--cap-add", "SYS_ADMIN"));
        Map<String, Boolean> bad = Run.probe(rootAdmin);
        line(out, "  hardened + root + SYS_ADMIN: " + count(bad).strip() + ": " + Probes.possibleNames(bad) + "  <- ROOT IS BACK, AND ONLY THE FILTER STILL STANDS BETWEEN IT AND MOUNT");
        List<String> unfiltered = new ArrayList<>(rootAdmin);
        unfiltered.set(unfiltered.indexOf("seccomp=" + seccomp), "seccomp=unconfined");
        Map<String, Boolean> worse = Run.probe(unfiltered);
        line(out, "  and without the filter as well: " + count(worse).strip() + ": " + Probes.possibleNames(worse) + "  <- EACH LAYER COVERS FOR A MISTAKE IN THE OTHERS");
    }

    private static void act5(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. The same on Kubernetes, with Pod Security Standards on kind.");
        try (Cluster cluster = new Cluster()) {
            line(out, "  a one-node kind cluster; three namespaces that enforce the privileged, baseline and restricted standards; three pods: the tutorial's, a plain one and the hardened one, sent with --dry-run=server so that the admission decides");
            cluster.namespace("shop-privileged", "privileged", "privileged");
            cluster.namespace("shop-baseline", "baseline", "restricted");
            cluster.namespace("shop-restricted", "restricted", "restricted");
            for (String[] pod : new String[][] {{"tutorial", "the tutorial's pod"}, {"plain", "a plain pod"}, {"hardened", "the hardened pod"}}) {
                String manifest = pod[0].equals("tutorial") ? Pods.tutorial("p") : pod[0].equals("plain") ? Pods.plain("p") : Pods.hardened("p");
                StringBuilder row = new StringBuilder(String.format("  %-18s", pod[1]));
                for (String ns : List.of("shop-privileged", "shop-baseline", "shop-restricted")) {
                    Sh.Result r = cluster.apply(ns, manifest, "--dry-run=server");
                    row.append(String.format(" %-11s %s;", ns.substring(5), r.ok() ? "admitted" : "refused (" + Cluster.violations(r.output()).size() + ")"));
                }
                line(out, row.toString());
            }
            Sh.Result refused = cluster.apply("shop-baseline", Pods.tutorial("p"), "--dry-run=server");
            line(out, "  baseline refuses the tutorial's pod for: " + Cluster.violations(refused.output()));
            Sh.Result refusedPlain = cluster.apply("shop-restricted", Pods.plain("p"), "--dry-run=server");
            line(out, "  restricted refuses a plain pod for: " + Cluster.violations(refusedPlain.output()));
            cluster.namespace("shop-warn", "privileged", "restricted");
            Sh.Result warned = cluster.apply("shop-warn", Pods.plain("plain"));
            line(out, "  a namespace that only warns: the plain pod is created: " + warned.output().contains("pod/plain created") + ", with a warning that lists " + Cluster.violations(warned.output()).size() + " violations");
            cluster.apply("shop-baseline", Pods.plain("plain"));
            cluster.apply("shop-restricted", Pods.hardened("hardened"));
            Map<String, Boolean> plain = cluster.probe("shop-baseline", "plain");
            Map<String, Boolean> hard = cluster.probe("shop-restricted", "hardened");
            line(out, "  the plain pod, admitted by baseline, probed from inside: " + count(plain).strip() + ": " + Probes.possibleNames(plain));
            line(out, "  the hardened pod, admitted by restricted: " + count(hard).strip() + ": " + Probes.possibleNames(hard));
        }
        line(out, "  the standards judge the pod's spec when it is created; a pod with no seccomp profile is admitted by baseline and runs unfiltered, and a writable temporary directory still runs code");
        line(out, "  whoever can edit the namespace's labels can change the standard; the secrets are still readable by the application; limits on processes belong to the kubelet, not to the pod");
        line(out, "  kind is a lab: its node is a container, and kind delete cluster takes the whole cluster away");
    }
}
