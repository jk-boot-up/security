package com.jk.explore.security.zerotrust;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The opa binary run as a server with one Rego file, asked for a decision over its REST API. */
final class Opa implements Decider, AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Process process;
    private final URI base;
    private final HttpClient http = HttpClient.newHttpClient();

    private Opa(Process process, URI base) {
        this.process = process;
        this.base = base;
    }

    /** Runs the Rego tests that sit beside the policy and returns {passed, total}. */
    static int[] test(Path policyDir) throws IOException, InterruptedException {
        Sh.Result r = Sh.run(policyDir, Map.of(), List.of("opa", "test", "."));
        Matcher m = Pattern.compile("PASS: (\\d+)/(\\d+)").matcher(r.output());
        if (!m.find()) {
            throw new IllegalStateException("opa test: " + r.output());
        }
        return new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
    }

    static Opa start(Path policy, Path log) throws IOException, InterruptedException {
        int port;
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = s.getLocalPort();
        }
        List<String> cmd = new ArrayList<>(List.of("opa", "run", "--server", "--addr", "127.0.0.1:" + port, policy.toString()));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        Opa opa = new Opa(p, URI.create("http://127.0.0.1:" + port));
        for (int i = 0; i < 100; i++) {
            try {
                if (opa.http.send(HttpRequest.newBuilder(opa.base.resolve("/health")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return opa;
                }
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        opa.close();
        throw new IllegalStateException("opa did not start");
    }

    @Override
    public Decision decide(Map<String, Object> input) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(base.resolve("/v1/data/bank/zerotrust/decision"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("input", input)))).build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode result = JSON.readTree(res.body()).path("result");
        if (res.statusCode() != 200 || !result.has("allow")) {
            throw new IOException("opa gave no decision");
        }
        List<String> reasons = new ArrayList<>();
        result.path("reasons").forEach(n -> reasons.add(n.asText()));
        return new Decision(result.path("allow").asBoolean(), reasons);
    }

    @Override
    public void close() {
        process.destroy();
    }
}
