package com.jk.explore.security.securitychaos;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A Toxiproxy container started from Java and driven through its HTTP API. Names and ports are never printed. */
final class Toxiproxy implements AutoCloseable {

    static final String IMAGE = "ghcr.io/shopify/toxiproxy";

    private final String container = "sc-toxi-" + UUID.randomUUID().toString().substring(0, 8);
    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final Map<String, Integer> hostPorts = new LinkedHashMap<>();
    private int apiPort;

    /** Starts the container with one proxy per dependency; each proxy forwards to the given port on this machine. */
    Toxiproxy(Map<String, Integer> upstreamPorts) throws IOException, InterruptedException {
        apiPort = freePort();
        List<String> cmd = new ArrayList<>(List.of("docker", "run", "-d", "--rm", "--name", container,
                "-p", "127.0.0.1:" + apiPort + ":8474"));
        int inside = 20000;
        Map<String, Integer> insidePorts = new LinkedHashMap<>();
        for (String name : upstreamPorts.keySet()) {
            int host = freePort();
            hostPorts.put(name, host);
            insidePorts.put(name, inside);
            cmd.addAll(List.of("-p", "127.0.0.1:" + host + ":" + inside));
            inside++;
        }
        cmd.add(IMAGE);
        try {
            Sh.must(cmd.toArray(String[]::new));
            waitForApi();
            for (String name : upstreamPorts.keySet()) {
                int status = call("POST", "/proxies", "{\"name\":\"" + name + "\",\"listen\":\"0.0.0.0:" + insidePorts.get(name)
                        + "\",\"upstream\":\"host.docker.internal:" + upstreamPorts.get(name) + "\",\"enabled\":true}");
                if (status != 201) {
                    throw new IllegalStateException("proxy " + name + " was not created: " + status);
                }
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            close();
            throw e;
        }
    }

    /** The port on this machine where a dependency is reached through its proxy. */
    int port(String name) {
        return hostPorts.get(name);
    }

    /** Injects the failure. */
    void inject(String proxy, Fault fault) throws IOException, InterruptedException {
        int status = fault == Fault.DOWN
                ? call("POST", "/proxies/" + proxy, "{\"enabled\":false}")
                : call("POST", "/proxies/" + proxy + "/toxics", fault.toxicJson());
        if (status != 200) {
            throw new IllegalStateException("fault " + fault + " was not injected: " + status);
        }
    }

    /** Enables every proxy and removes every toxic. */
    void reset() throws IOException, InterruptedException {
        if (call("POST", "/reset", "") != 204) {
            throw new IllegalStateException("reset failed");
        }
    }

    private void waitForApi() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            try {
                if (call("GET", "/version", null) == 200) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Toxiproxy did not start");
    }

    private int call(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + apiPort + path)).timeout(Duration.ofSeconds(10));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(b.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    @Override
    public void close() {
        try {
            Sh.run(List.of("docker", "rm", "-f", container));
        } catch (IOException e) {
            // nothing more to do
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        client.close();
    }
}
