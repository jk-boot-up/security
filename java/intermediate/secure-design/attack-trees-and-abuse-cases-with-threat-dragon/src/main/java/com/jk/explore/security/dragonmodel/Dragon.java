package com.jk.explore.security.dragonmodel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The real OWASP Threat Dragon image: its own validator for model files, and its own server and editor. */
public final class Dragon implements AutoCloseable {

    /** The image for this machine: the project publishes a separate tag for arm64. */
    static final String IMAGE = "owasp/threat-dragon:v2.6.2" + (System.getProperty("os.arch").contains("aarch64") || System.getProperty("os.arch").contains("arm64") ? "-arm64" : "");

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** What the editor's validator said about one file. */
    public record Verdict(boolean valid, List<String> errors) {
    }

    private final Path work;
    private String container;
    private int port;

    public Dragon() throws IOException {
        this.work = Files.createTempDirectory("dragon");
        try (InputStream in = Dragon.class.getResourceAsStream("/validate-model.js")) {
            Files.write(work.resolve("validate-model.js"), in.readAllBytes());
        }
    }

    /** Writes the files and has the image's own Ajv, with the schema from its own editor bundle, check them all in one container. */
    public Map<String, Verdict> validate(Map<String, String> files) throws IOException, InterruptedException {
        for (Map.Entry<String, String> f : files.entrySet()) {
            Files.writeString(work.resolve(f.getKey()), f.getValue());
        }
        List<String> command = new ArrayList<>(List.of("docker", "run", "--rm", "-v", work + ":/m:ro", "--entrypoint", "node", IMAGE, "/m/validate-model.js"));
        files.keySet().forEach(name -> command.add("/m/" + name));
        String out = Sh.run(command);
        JsonNode result = JSON.readTree(out.lines().filter(l -> l.startsWith("{")).findFirst().orElseThrow());
        Map<String, Verdict> verdicts = new LinkedHashMap<>();
        for (String name : files.keySet()) {
            List<String> errors = new ArrayList<>();
            result.get(name).get("errors").forEach(e -> errors.add(e.asString()));
            verdicts.put(name, new Verdict(result.get(name).get("valid").asBoolean(), errors));
        }
        return verdicts;
    }

    /** Starts the editor's server with generated keys, in development mode so that it speaks plain http on a loopback port. */
    public void startServer() throws IOException, InterruptedException {
        SecureRandom random = new SecureRandom();
        String key = HexFormat.of().formatHex(random.generateSeed(16));
        container = "dragon-" + UUID.randomUUID().toString().substring(0, 8);
        Sh.run(List.of("docker", "run", "-d", "--rm", "--name", container, "-p", "127.0.0.1::3000", "-e", "ENCRYPTION_KEYS=[{\"isPrimary\":true,\"id\":0,\"value\":\"" + key + "\"}]",
                "-e", "ENCRYPTION_JWT_SIGNING_KEY=" + HexFormat.of().formatHex(random.generateSeed(16)), "-e", "ENCRYPTION_JWT_REFRESH_SIGNING_KEY=" + HexFormat.of().formatHex(random.generateSeed(16)),
                "-e", "NODE_ENV=development", "-e", "SERVER_API_PROTOCOL=http", IMAGE));
        String mapping = Sh.run(List.of("docker", "port", container, "3000")).lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElseThrow();
        port = Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).strip());
        for (int i = 0; i < 100; i++) {
            try {
                if (get("/healthz").status() == 200) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("threat dragon did not start");
    }

    /** What the server answered. */
    public record Reply(int status, String body) {
    }

    public Reply get(String path) throws IOException, InterruptedException {
        HttpResponse<String> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), r.body());
    }

    @Override
    public void close() {
        if (container != null) {
            try {
                Sh.run(List.of("docker", "rm", "-f", container));
            } catch (IOException | InterruptedException | IllegalStateException e) {
                // already gone
            }
        }
    }
}
