package com.jk.explore.security.lokilogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** A real Loki 3 (with its rule evaluator) and a real Grafana 13 on a private Docker network, each bound to a free loopback port and removed on close. */
public final class Lab implements AutoCloseable {

    static final String LOKI_IMAGE = "grafana/loki:3.7.9";
    static final String GRAFANA_IMAGE = "grafana/grafana:13.2.4";

    static final String LOKI_CONFIG = """
            auth_enabled: true
            server:
              http_listen_port: 3100
              grpc_listen_port: 9096
            common:
              instance_addr: 127.0.0.1
              path_prefix: /loki
              storage:
                filesystem:
                  chunks_directory: /loki/chunks
                  rules_directory: /loki/rules
              replication_factor: 1
              ring:
                kvstore:
                  store: inmemory
            ingester:
              lifecycler:
                min_ready_duration: 0s
            schema_config:
              configs:
                - from: 2024-01-01
                  store: tsdb
                  object_store: filesystem
                  schema: v13
                  index:
                    prefix: index_
                    period: 24h
            limits_config:
              reject_old_samples: true
              reject_old_samples_max_age: 168h
              allow_structured_metadata: true
            ruler:
              storage:
                type: local
                local:
                  directory: /loki/rules
              rule_path: /tmp/loki-rules-scratch
              evaluation_interval: 2s
              poll_interval: 2s
              enable_api: true
              ring:
                kvstore:
                  store: inmemory
            analytics:
              reporting_enabled: false
            """;

    /** Two rules for the tenant bank: one that works, and one with a typo in the field name that will never match. */
    static final String RULES = """
            groups:
              - name: bank-logins
                interval: 2s
                rules:
                  - alert: PasswordGuessing
                    expr: sum by (source_ip) (count_over_time({app="bank"} | json | outcome="failure" [5m])) >= 3
                    for: 0s
                    labels:
                      severity: high
                    annotations:
                      summary: "{{ $labels.source_ip }} failed to log in 3 times or more in 5 minutes"
                  - alert: PasswordGuessingTypo
                    expr: sum by (source_ip) (count_over_time({app="bank"} | json | outcom="failure" [5m])) >= 3
                    for: 0s
                    labels:
                      severity: high
            """;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String network = "logs-net-" + suffix;
    private final String loki = "logs-loki-" + suffix;
    private final String grafana = "logs-grafana-" + suffix;
    private final int lokiPort;
    private final int grafanaPort;

    public Lab() throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("lokilogs");
        Files.writeString(dir.resolve("loki.yaml"), LOKI_CONFIG);
        Files.createDirectories(dir.resolve("rules/bank"));
        Files.writeString(dir.resolve("rules/bank/rules.yaml"), RULES);
        run("docker", "network", "create", network);
        try {
            run("docker", "run", "-d", "--rm", "--name", loki, "--network", network, "--network-alias", "loki", "-p", "127.0.0.1::3100", "-v", dir.resolve("loki.yaml") + ":/etc/loki/loki.yaml:ro",
                    "-v", dir.resolve("rules") + ":/loki/rules:ro", LOKI_IMAGE, "-config.file=/etc/loki/loki.yaml");
            run("docker", "run", "-d", "--rm", "--name", grafana, "--network", network, "-p", "127.0.0.1::3000", "-e", "GF_SECURITY_ADMIN_PASSWORD=lab-only", "-e", "GF_ANALYTICS_REPORTING_ENABLED=false",
                    "-e", "GF_ANALYTICS_CHECK_FOR_UPDATES=false", GRAFANA_IMAGE);
            this.lokiPort = port(loki, "3100");
            this.grafanaPort = port(grafana, "3000");
            waitFor("http://127.0.0.1:" + lokiPort + "/ready", "ready");
            waitFor("http://127.0.0.1:" + grafanaPort + "/api/health", "ok");
        } catch (IOException | InterruptedException | RuntimeException e) {
            close();
            throw e;
        }
    }

    String lokiUrl() {
        return "http://127.0.0.1:" + lokiPort;
    }

    String grafanaUrl() {
        return "http://127.0.0.1:" + grafanaPort;
    }

    private static int port(String container, String port) throws IOException, InterruptedException {
        String mapping = run("docker", "port", container, port).lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElseThrow();
        return Integer.parseInt(mapping.substring(mapping.lastIndexOf(':') + 1).strip());
    }

    private static void waitFor(String url, String contains) throws InterruptedException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        for (int i = 0; i < 300; i++) {
            try {
                var r = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200 && r.body().contains(contains)) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException(url + " did not come up");
    }

    private static String run(String... command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }

    @Override
    public void close() {
        for (List<String> command : List.of(List.of("docker", "rm", "-f", grafana), List.of("docker", "rm", "-f", loki), List.of("docker", "network", "rm", network))) {
            try {
                run(command.toArray(String[]::new));
            } catch (IOException | InterruptedException | IllegalStateException e) {
                // already gone
            }
        }
    }
}
