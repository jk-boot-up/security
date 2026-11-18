package com.jk.explore.security.containerhard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Starts the shop's image with different docker run options and a kind cluster with Pod Security Standards, runs thirteen real attempts inside each
 * container, and prints the results in six numbered acts. Each act prints its input (the options or the pods) and its result (the attempts, the
 * admission decisions) in separate labelled sections; container and cluster names are never printed, so the output is the same on every run.
 */
public class ContainerHardeningDemo {

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

    static final String IMAGE = "shop-app:probe";

    public static String run() throws Exception {
        Report out = new Report();
        Image.build();
        String seccomp = Seccomp.profile().toString();
        act1(out, seccomp);
        Map<String, Boolean> tutorial = act2(out, seccomp);
        act3(out, seccomp, tutorial);
        Map<String, Boolean> hardened = act4(out, seccomp);
        act5(out, seccomp, hardened);
        act6(out);
        return out.toString();
    }

    private static void act1(Report out, String seccomp) throws IOException, InterruptedException {
        out.line("ONE. What docker run with no options gives an attacker.");
        out.line("  THE LAB:");
        out.line("    image    " + IMAGE + ", from " + Image.BASE);
        out.line("    users    app (10001) owns /app; the image runs as root");
        out.line("    secret   DB_PASSWORD, in the environment");
        out.line("    probes   13 shell attempts, run inside the container");
        out.line("    seccomp  a profile file that denies " + Seccomp.denied() + " system calls,");
        out.line("             the ones Docker's own default profile denies");
        out.line("  THE OPTIONS (only the filter, as Docker would add it):");
        out.command(stage("default", "<profile>"), IMAGE);
        String raw = Run.probeOutput(stage("default", seccomp));
        Map<String, String> facts = Probes.facts(raw);
        out.line("  WHAT THE PROBES READ:");
        out.facts(facts);
        out.line("  HOW THE CAPABILITY MASK IS READ (CapEff " + facts.get("CapEff") + "):");
        out.capabilityBits(facts.get("CapEff"));
        Map<String, Boolean> r = Probes.parse(raw);
        out.line("  THE ATTEMPTS, BY WHAT THEY REACH:");
        out.attempts(r);
        out.line("  HOW THE COUNT IS MADE:");
        out.count(r);
    }

    private static Map<String, Boolean> act2(Report out, String seccomp) throws IOException, InterruptedException {
        out.line("TWO. The configuration from the tutorial.");
        out.line("  THE OPTIONS:");
        out.command(stage("tutorial", seccomp), IMAGE);
        Map<String, Boolean> r = Run.probe(stage("tutorial", seccomp));
        out.line("  THE ATTEMPTS, BY WHAT THEY REACH:");
        out.attempts(r);
        out.line("  HOW THE COUNT IS MADE:");
        out.count(r);
        return r;
    }

    private static void act3(Report out, String seccomp, Map<String, Boolean> tutorial) throws IOException, InterruptedException {
        out.line("THREE. One setting at a time.");
        out.line("  BEFORE THE STEPS:");
        out.line(String.format("    %-33s %2d of %d", "the tutorial configuration", Probes.possible(tutorial), Probes.NAMES.length));
        out.line("  THE STEPS (one option more each time):");
        String[][] stages = {{"unprivileged", "no host access, not privileged"}, {"default", "a seccomp profile"}, {"no-caps", "drop all capabilities"},
                {"non-root", "a user that is not root"}, {"no-new-privileges", "no new privileges"}, {"read-only", "read-only root, noexec /tmp"},
                {"limits", "memory and process limits"}};
        Map<String, Boolean> before = tutorial;
        for (int i = 0; i < stages.length; i++) {
            Map<String, Boolean> after = Run.probe(stage(stages[i][0], seccomp));
            out.step(i + 1, stages[i][1], before, after);
            before = after;
        }
        out.line("  AFTER THE STEPS:");
        out.wrap("    " + Probes.possible(before) + " of " + Probes.NAMES.length + " possible: ", Probes.possibleNames(before));
        out.line("  WHY STEPS 3, 4 AND 5 LOOK ODD:");
        out.line("    3: root without DAC_OVERRIDE cannot write app's files");
        out.line("    4: user app owns /app, so it can change it again");
        out.line("    5: the image has no set-user-id program to stop");
    }

    private static Map<String, Boolean> act4(Report out, String seccomp) throws IOException, InterruptedException {
        out.line("FOUR. The hardened container.");
        out.line("  THE OPTIONS:");
        out.command(stage("limits", "<profile>"), IMAGE);
        String raw = Run.probeOutput(stage("limits", seccomp));
        out.line("  WHAT THE PROBES READ:");
        out.facts(Probes.facts(raw));
        Map<String, Boolean> r = Probes.parse(raw);
        out.line("  THE ATTEMPTS, BY WHAT THEY REACH:");
        out.attempts(r);
        out.line("  HOW THE COUNT IS MADE:");
        out.count(r);
        return r;
    }

    private static void act5(Report out, String seccomp, Map<String, Boolean> hardened) throws IOException, InterruptedException {
        out.line("FIVE. Settings that look harmless.");
        out.line("  BEFORE: the hardened container");
        out.line(String.format("    %d of %d possible", Probes.possible(hardened), Probes.NAMES.length));
        List<String> netRaw = new ArrayList<>(stage("limits", seccomp));
        netRaw.addAll(List.of("--cap-add", "NET_RAW"));
        Map<String, Boolean> withNetRaw = Run.probe(netRaw);
        out.line("  AFTER: hardened + --cap-add NET_RAW");
        out.changed(hardened, withNetRaw);
        out.line(String.format("    %-19s %-8s %s", "sniff-network", withNetRaw.get("sniff-network") ? "POSSIBLE" : "blocked", "NET_RAW works only for root"));
        List<String> rootAdmin = new ArrayList<>(stage("limits", seccomp));
        rootAdmin.addAll(List.of("--user", "0", "--cap-add", "SYS_ADMIN"));
        Map<String, Boolean> admin = Run.probe(rootAdmin);
        out.line("  AFTER: hardened + --user 0 --cap-add SYS_ADMIN");
        out.changed(hardened, admin);
        out.line(String.format("    %-19s %-8s %s", "mount-host", admin.get("mount-host") ? "POSSIBLE" : "blocked", "the seccomp profile denies mount"));
        List<String> unfiltered = new ArrayList<>(rootAdmin);
        unfiltered.set(unfiltered.indexOf("seccomp=" + seccomp), "seccomp=unconfined");
        Map<String, Boolean> worse = Run.probe(unfiltered);
        out.line("  AFTER: the same, and seccomp=unconfined");
        out.changed(hardened, worse);
        out.line("  WHAT IT SHOWS:");
        out.line("    each layer covers for a mistake in the others");
    }

    private static void act6(Report out) throws IOException, InterruptedException {
        out.line("SIX. The same on Kubernetes, with Pod Security Standards on kind.");
        out.line("  THE LAB:");
        out.line("    cluster     one kind node, itself a container");
        out.line("    namespaces  enforce privileged, baseline, restricted");
        out.line("    pods        the tutorial's, a plain one, the hardened one");
        out.line("    how         kubectl apply --dry-run=server: only the");
        out.line("                admission decides, nothing is created");
        try (Cluster cluster = new Cluster()) {
            cluster.namespace("shop-privileged", "privileged", "privileged");
            cluster.namespace("shop-baseline", "baseline", "restricted");
            cluster.namespace("shop-restricted", "restricted", "restricted");
            out.line("  THE ADMISSION DECISIONS (refused: how many checks fail):");
            out.admission("pod", List.of("privileged", "baseline", "restricted"));
            for (String[] pod : new String[][] {{"tutorial", "tutorial's"}, {"plain", "plain"}, {"hardened", "hardened"}}) {
                String manifest = pod[0].equals("tutorial") ? Pods.tutorial("p") : pod[0].equals("plain") ? Pods.plain("p") : Pods.hardened("p");
                List<String> decisions = new ArrayList<>();
                for (String ns : List.of("shop-privileged", "shop-baseline", "shop-restricted")) {
                    Sh.Result r = cluster.apply(ns, manifest, "--dry-run=server");
                    decisions.add(r.ok() ? "admitted" : "refused (" + Cluster.violations(r.output()).size() + ")");
                }
                out.admission(pod[1], decisions);
            }
            out.line("  WHY BASELINE REFUSES THE TUTORIAL'S POD:");
            Cluster.violations(cluster.apply("shop-baseline", Pods.tutorial("p"), "--dry-run=server").output()).forEach(v -> out.line("    " + v));
            out.line("  WHY RESTRICTED REFUSES THE PLAIN POD:");
            Cluster.violations(cluster.apply("shop-restricted", Pods.plain("p"), "--dry-run=server").output()).forEach(v -> out.line("    " + v));
            cluster.namespace("shop-warn", "privileged", "restricted");
            Sh.Result warned = cluster.apply("shop-warn", Pods.plain("plain"));
            out.line("  A NAMESPACE THAT ONLY WARNS (enforce privileged):");
            out.line("    the plain pod is created: " + (warned.output().contains("pod/plain created") ? "yes" : "no"));
            out.line("    the warning lists " + Cluster.violations(warned.output()).size() + " restricted checks");
            cluster.apply("shop-baseline", Pods.plain("plain"));
            cluster.apply("shop-restricted", Pods.hardened("hardened"));
            Map<String, Boolean> plain = cluster.probe("shop-baseline", "plain");
            Map<String, Boolean> hard = cluster.probe("shop-restricted", "hardened");
            out.line("  THE RUNNING PODS, PROBED FROM INSIDE:");
            out.line(String.format("    plain, in baseline       %2d of %d", Probes.possible(plain), Probes.NAMES.length));
            out.wrap("      ", Probes.possibleNames(plain));
            out.line(String.format("    hardened, in restricted  %2d of %d", Probes.possible(hard), Probes.NAMES.length));
            out.wrap("      ", Probes.possibleNames(hard));
        }
        out.line("  WHAT THE STANDARDS DO NOT COVER:");
        out.line("    they judge the pod's spec once, when it is created");
        out.line("    a writable emptyDir /tmp still runs a dropped script");
        out.line("    whoever can edit the namespace labels sets the standard");
        out.line("    the application can still read its own secrets");
    }
}
