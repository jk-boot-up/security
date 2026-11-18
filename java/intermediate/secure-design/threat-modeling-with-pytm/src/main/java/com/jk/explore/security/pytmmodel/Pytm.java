package com.jk.explore.security.pytmmodel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The real pytm 1.4, installed with pip into a virtual environment under build/ the first time, and run on a model file. */
public final class Pytm implements ThreatTool {

    static final String VERSION = "1.4.0";
    /** pytm and every package it pulls in, at exact versions, so the findings cannot change under the demo. */
    static final List<String> PINNED = List.of("pytm==" + VERSION, "pydantic==2.14.1", "pydantic-core==2.50.1", "annotated-types==0.8.0",
            "typing-extensions==4.16.0", "typing-inspection==0.4.4");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** What a run printed and whether it worked. */
    public record Run(int exit, String output) {
    }

    private final Path venv;
    private final Path work;

    public Pytm(Path buildDir) throws IOException, InterruptedException {
        this.venv = buildDir.toAbsolutePath().resolve("pytm-venv");
        this.work = Files.createDirectories(buildDir.toAbsolutePath().resolve("pytm-work"));
        Path marker = venv.resolve("bin/pytm-installed-" + Integer.toHexString(PINNED.hashCode()));
        if (!Files.exists(marker)) {
            exec(List.of("python3", "-m", "venv", venv.toString()), work);
            List<String> pip = new ArrayList<>(List.of(venv.resolve("bin/python").toString(), "-m", "pip", "install", "--quiet"));
            pip.addAll(PINNED);
            Run r = exec(pip, work);
            if (r.exit() != 0) {
                throw new IllegalStateException("pip failed: " + r.output());
            }
            Files.writeString(marker, String.join("\n", PINNED));
        }
    }

    private static Run exec(List<String> command, Path dir) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(false).start();
        String out = new String(p.getInputStream().readAllBytes());
        String err = new String(p.getErrorStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("did not finish: " + command);
        }
        return new Run(p.exitValue(), p.exitValue() == 0 ? out : out + err);
    }

    /** Runs a model file with pytm's own options, in a folder of its own. */
    public Run model(String name, String python, String... options) throws IOException, InterruptedException {
        Path file = work.resolve(name + ".py");
        Files.writeString(file, python);
        List<String> command = new ArrayList<>(List.of(venv.resolve("bin/python").toString(), file.toString()));
        command.addAll(List.of(options));
        return exec(command, work);
    }

    @Override
    public List<Threat> library() throws IOException {
        Path json;
        try (Stream<Path> found = Files.find(venv.resolve("lib"), 6, (p, a) -> p.endsWith("pytm/threatlib/threats.json"))) {
            json = found.findFirst().orElseThrow();
        }
        List<Threat> out = new ArrayList<>();
        for (JsonNode t : JSON.readTree(Files.readString(json))) {
            if (t.has("DEPRECATED")) {
                continue;
            }
            List<String> targets = new ArrayList<>();
            if (t.get("target").isArray()) {
                t.get("target").forEach(x -> targets.add(x.asString()));
            } else {
                targets.add(t.get("target").asString());
            }
            out.add(new Threat(t.get("SID").asString(), targets, t.get("severity").asString(), t.get("description").asString(), t.get("condition").asString()));
        }
        return out;
    }

    /** The number of threats pytm itself lists with --list. */
    public int listed(String python) throws IOException, InterruptedException {
        return (int) model("library", python, "--list").output().lines().filter(l -> l.matches("^[A-Z]+[0-9]+ - .*")).count();
    }

    @Override
    public List<Finding> findings(String name, String python) throws IOException, InterruptedException {
        Path json = work.resolve(name + ".json");
        Run r = model(name, python, "--json", json.toString());
        if (r.exit() != 0) {
            throw new IllegalStateException("pytm failed: " + r.output());
        }
        List<Finding> out = new ArrayList<>();
        for (JsonNode f : JSON.readTree(Files.readString(json)).get("findings")) {
            out.add(new Finding(f.get("threat_id").asString(), f.get("target").asString(), f.get("severity").asString(), f.get("description").asString()));
        }
        return out;
    }

    @Override
    public String dfd(String name, String python) throws IOException, InterruptedException {
        return model(name, python, "--dfd").output();
    }

    @Override
    public String report(String name, String python, String template) throws IOException, InterruptedException {
        Path file = Files.writeString(work.resolve(name + "-template.md"), template);
        return model(name, python, "--report", file.toString()).output();
    }

    /** Draws the diagram with Graphviz (dot -Tsvg) and returns the SVG file. */
    public Path render(String name, String dot) throws IOException, InterruptedException {
        Path dotFile = Files.writeString(work.resolve(name + ".dot"), dot);
        Path svg = work.resolve(name + ".svg");
        Run r = exec(List.of("dot", "-Tsvg", "-o", svg.toString(), dotFile.toString()), work);
        if (r.exit() != 0) {
            throw new IllegalStateException("dot failed: " + r.output());
        }
        return svg;
    }

    public Path work() {
        return work;
    }
}
