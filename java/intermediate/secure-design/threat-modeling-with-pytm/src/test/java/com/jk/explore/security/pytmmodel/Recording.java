package com.jk.explore.security.pytmmodel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Passes every call to the real pytm and writes what it answered in the format Recorded reads. */
final class Recording implements ThreatTool {

    private final ThreatTool pytm;
    private final Path dir;

    Recording(ThreatTool pytm, Path dir) throws IOException {
        this.pytm = pytm;
        this.dir = Files.createDirectories(dir);
        Files.deleteIfExists(dir.resolve("models.tsv"));
    }

    private void model(String name, String input) throws IOException {
        Files.writeString(dir.resolve("models.tsv"), name + "\t" + Recorded.sha256(input) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public List<Threat> library() throws Exception {
        List<Threat> out = pytm.library();
        StringBuilder text = new StringBuilder();
        for (Threat t : out) {
            text.append(String.join("\t", t.id(), String.join(",", t.targets()), t.severity(), t.title(), t.condition())).append('\n');
        }
        Files.writeString(dir.resolve("threats.tsv"), text);
        return out;
    }

    @Override
    public List<Finding> findings(String name, String python) throws Exception {
        List<Finding> out = pytm.findings(name, python);
        StringBuilder text = new StringBuilder();
        for (Finding f : out) {
            text.append(String.join("\t", f.threat(), f.target(), f.severity(), f.title())).append('\n');
        }
        Files.writeString(dir.resolve(name + ".tsv"), text);
        model(name, python);
        return out;
    }

    @Override
    public String dfd(String name, String python) throws Exception {
        String out = pytm.dfd(name, python);
        Files.writeString(dir.resolve(name + ".dot"), out);
        model(name, python);
        return out;
    }

    @Override
    public String report(String name, String python, String template) throws Exception {
        String out = pytm.report(name, python, template);
        Files.writeString(dir.resolve(name + ".md"), out);
        model(name, python + template);
        return out;
    }
}
