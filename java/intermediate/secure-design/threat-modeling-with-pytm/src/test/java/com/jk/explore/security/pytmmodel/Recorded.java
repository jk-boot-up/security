package com.jk.explore.security.pytmmodel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * pytm as recorded in one real run (RecordingsTest writes and compares the files): the library, and for each model its findings, diagram or report.
 * Each recording keeps the SHA-256 of the model it was made from, so a model the Writer no longer writes is refused instead of answered.
 */
final class Recorded implements ThreatTool {

    static final Path DIR = Path.of("src", "test", "resources", "recorded");

    private final Path dir;
    private final Map<String, String> models = new LinkedHashMap<>();

    Recorded(Path dir) throws IOException {
        this.dir = dir;
        for (String l : Files.readAllLines(dir.resolve("models.tsv"))) {
            String[] p = l.split("\t");
            models.put(p[0], p[1]);
        }
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void same(String name, String input) {
        if (!sha256(input).equals(models.get(name))) {
            throw new IllegalStateException("no recording of this model: " + name + "; run ./gradlew integrationTest and copy build/recorded");
        }
    }

    @Override
    public List<Threat> library() throws IOException {
        List<Threat> out = new ArrayList<>();
        for (String l : Files.readAllLines(dir.resolve("threats.tsv"))) {
            String[] p = l.split("\t");
            out.add(new Threat(p[0], List.of(p[1].split(",")), p[2], p[3], p[4]));
        }
        return out;
    }

    @Override
    public List<Finding> findings(String name, String python) throws IOException {
        same(name, python);
        List<Finding> out = new ArrayList<>();
        for (String l : Files.readAllLines(dir.resolve(name + ".tsv"))) {
            String[] p = l.split("\t");
            out.add(new Finding(p[0], p[1], p[2], p[3]));
        }
        return out;
    }

    @Override
    public String dfd(String name, String python) throws IOException {
        same(name, python);
        return Files.readString(dir.resolve(name + ".dot"));
    }

    @Override
    public String report(String name, String python, String template) throws IOException {
        same(name, python + template);
        return Files.readString(dir.resolve(name + ".md"));
    }
}
