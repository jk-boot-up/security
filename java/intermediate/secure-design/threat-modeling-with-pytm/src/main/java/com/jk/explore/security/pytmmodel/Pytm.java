package com.jk.explore.security.pytmmodel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The real pytm 1.4, installed with pip into a virtual environment under build/ the first time, and run on a model file. */
public final class Pytm {

    static final String VERSION = "1.4.0";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** One finding: the threat of pytm's library, the element or flow it is about, how severe pytm rates it, and its title. */
    public record Finding(String threat, String target, String severity, String title) {
    }

    /** What a run printed and whether it worked. */
    public record Run(int exit, String output) {
    }

    private final Path venv;
    private final Path work;

    public Pytm(Path buildDir) throws IOException, InterruptedException {
        this.venv = buildDir.toAbsolutePath().resolve("pytm-venv");
        this.work = Files.createDirectories(buildDir.toAbsolutePath().resolve("pytm-work"));
        Path python = venv.resolve("bin/python");
        if (!Files.exists(venv.resolve("bin/pytm-installed-" + VERSION))) {
            exec(List.of("python3", "-m", "venv", venv.toString()), work);
            exec(List.of(python.toString(), "-m", "pip", "install", "--quiet", "pytm==" + VERSION), work);
            Files.writeString(venv.resolve("bin/pytm-installed-" + VERSION), VERSION);
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

    /** The findings pytm reports for a model. */
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

    /** The graph description (dot) of the data-flow diagram. */
    public String dfd(String name, String python) throws IOException, InterruptedException {
        return model(name, python, "--dfd").output();
    }

    /** The number of threats in pytm's library. */
    public int libraryThreats(String python) throws IOException, InterruptedException {
        return (int) model("library", python, "--list").output().lines().filter(l -> l.matches("^[A-Z]+[0-9]+ - .*")).count();
    }

    public Path work() {
        return work;
    }
}
