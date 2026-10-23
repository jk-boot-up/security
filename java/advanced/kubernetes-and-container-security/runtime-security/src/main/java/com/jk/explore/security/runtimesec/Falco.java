package com.jk.explore.security.runtimesec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Falco, run as a container: first to check the rules file, then to try its driver on this machine's kernel. */
public final class Falco {

    public static final String IMAGE = "falcosecurity/falco:0.45.1";

    private Falco() {
    }

    /** Whether Falco accepts the rules file (together with its own default rules, which define the macros the rules use). */
    public static boolean validates(Path rulesDir, String file) throws IOException, InterruptedException {
        return Sh.run(List.of("docker", "run", "--rm", "-v", rulesDir.toAbsolutePath() + ":/r:ro", IMAGE, "falco",
                "--validate", "/etc/falco/falco_rules.yaml", "--validate", "/r/" + file)).ok();
    }

    /** The number of rules in a Falco rules file. */
    public static long countRules(String yaml) {
        return yaml.lines().filter(l -> l.startsWith("- rule:")).count();
    }

    /** Starts Falco with its modern eBPF driver for a few seconds: the line that says why it stopped, or "started". */
    public static String tryDriver() throws IOException, InterruptedException {
        Sh.Result r = Sh.run(List.of("docker", "run", "--rm", "--privileged", "--pid=host", "-v", "/proc:/host/proc:ro", "-v", "/etc:/host/etc:ro",
                IMAGE, "falco", "-o", "engine.kind=modern_ebpf", "-M", "5"));
        return r.output().lines().filter(l -> l.startsWith("libpman:")).findFirst().orElse(r.ok() ? "started" : "stopped for another reason");
    }

    /** Whether the kernel of the Docker VM has the type information (BTF) that the modern eBPF driver needs. */
    public static boolean kernelHasBtf() throws IOException, InterruptedException {
        return Sh.run("docker", "run", "--rm", "alpine:3.22", "test", "-e", "/sys/kernel/btf/vmlinux").ok();
    }
}
