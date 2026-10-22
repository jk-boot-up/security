package com.jk.explore.security.netiso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Real Docker networks and containers: each service is a container of the alpine image running busybox nc on its port (listening for ever, one connection after another), attached to the networks the layout gives it.
 * Reachability is measured from inside a container with nc, by service name and by address.
 */
public final class Lab implements AutoCloseable {

    static final String IMAGE = "alpine:3.24.3";

    private final String suffix = UUID.randomUUID().toString().substring(0, 6);
    private final List<String> networks = new ArrayList<>();
    private final Map<String, String> containers = new LinkedHashMap<>();
    private final Map<String, Service> services = new LinkedHashMap<>();

    public String networkName(String name) {
        return name + "-" + suffix;
    }

    public String containerName(String service) {
        return containers.get(service);
    }

    /** Makes a bridge network; {@code internal} networks have no route to the outside. */
    public void network(String name, boolean internal) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("network", "create"));
        if (internal) {
            command.add("--internal");
        }
        command.add(networkName(name));
        Sh.must(command.toArray(String[]::new));
        networks.add(networkName(name));
    }

    /** Starts a service on its first network; further networks are joined with {@link #join}. */
    public void start(Service service, String firstNetwork, boolean publish) throws IOException, InterruptedException {
        String container = "iso-" + service.name() + "-" + suffix;
        List<String> command = new ArrayList<>(List.of("run", "-d", "--rm", "--name", container, "--network", networkName(firstNetwork), "--network-alias", service.name()));
        if (publish) {
            command.addAll(List.of("-p", "127.0.0.1::" + service.port()));
        }
        command.addAll(List.of(IMAGE, "nc", "-ll", "-p", String.valueOf(service.port()), "-e", "/bin/true"));
        Sh.must(command.toArray(String[]::new));
        containers.put(service.name(), container);
        services.put(service.name(), service);
    }

    public void join(String service, String network) throws IOException, InterruptedException {
        Sh.must("network", "connect", "--alias", service, networkName(network), containers.get(service));
    }

    public void leave(String service, String network) throws IOException, InterruptedException {
        Sh.must("network", "disconnect", networkName(network), containers.get(service));
    }

    /** The addresses of a container, one for each network it is on. */
    public List<String> addresses(String service) throws IOException, InterruptedException {
        String out = Sh.must("inspect", "-f", "{{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}", containers.get(service));
        return List.of(out.strip().split("\\s+"));
    }

    /** Whether {@code from} can open a connection to {@code host}:{@code port}, within a second. */
    public boolean connects(String from, String host, int port) throws IOException, InterruptedException {
        return Sh.run("exec", containers.get(from), "nc", "-z", "-w", "1", host, String.valueOf(port)).ok();
    }

    /** The services that {@code from} reaches by name, on their own ports. */
    public List<String> reachesByName(String from) throws IOException, InterruptedException {
        List<String> out = new ArrayList<>();
        for (Service s : services.values()) {
            if (!s.name().equals(from) && connects(from, s.name(), s.port())) {
                out.add(s.name());
            }
        }
        return out;
    }

    /** The services that {@code from} reaches by the address they have on any network, scanning all of them: what an attacker who has learned the addresses can do. */
    public List<String> reachesByAddress(String from) throws IOException, InterruptedException {
        Map<String, java.util.concurrent.CompletableFuture<Boolean>> futures = new LinkedHashMap<>();
        for (Service s : services.values()) {
            if (s.name().equals(from)) {
                continue;
            }
            List<String> addresses = addresses(s.name());
            futures.put(s.name(), java.util.concurrent.CompletableFuture.supplyAsync(() -> addresses.stream().parallel().anyMatch(a -> {
                try {
                    return connects(from, a, s.port());
                } catch (IOException | InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            })));
        }
        List<String> out = new ArrayList<>();
        futures.forEach((k, v) -> {
            if (v.join()) {
                out.add(k);
            }
        });
        return out;
    }

    /** The whole matrix by name: for every service the services it reaches. */
    public Map<String, List<String>> matrix() throws IOException, InterruptedException {
        Map<String, java.util.concurrent.CompletableFuture<List<String>>> futures = new LinkedHashMap<>();
        for (String from : containers.keySet()) {
            futures.put(from, java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return reachesByName(from);
                } catch (IOException | InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }));
        }
        Map<String, List<String>> out = new LinkedHashMap<>();
        futures.forEach((k, v) -> out.put(k, v.join()));
        return out;
    }

    /** The unordered pairs, such as app-web, between which a connection works in at least one direction. */
    public java.util.Set<String> pairs() throws IOException, InterruptedException {
        java.util.Set<String> out = new TreeSet<>();
        for (Map.Entry<String, List<String>> e : matrix().entrySet()) {
            for (String to : e.getValue()) {
                out.add(Intent.pair(e.getKey(), to));
            }
        }
        return out;
    }

    public java.util.Set<String> names() {
        return containers.keySet();
    }

    /** Builds the layout that an intent asks for: one network for each allowed pair, internal, and every service on the networks of its pairs. */
    public void build(Intent intent, boolean publishWeb) throws IOException, InterruptedException {
        Map<String, List<String>> networksOf = new LinkedHashMap<>();
        int n = 0;
        for (String pair : intent.pairs()) {
            String net = "link" + (++n);
            network(net, true);
            for (String member : pair.split("-")) {
                networksOf.computeIfAbsent(member, k -> new ArrayList<>()).add(net);
            }
        }
        if (publishWeb) {
            network("edge", false);
        }
        for (Service s : Service.shop()) {
            List<String> nets = networksOf.get(s.name());
            boolean front = publishWeb && s.name().equals("web");
            // a published port needs a network that is not internal: the front starts on the edge network and joins its links
            start(s, front ? "edge" : nets.get(0), front);
            for (String more : front ? nets : nets.subList(1, nets.size())) {
                join(s.name(), more);
            }
        }
    }

    /** The host-side port that Docker published for a service, or -1. */
    public int publishedPort(String service) throws IOException, InterruptedException {
        Sh.Result r = Sh.run("port", containers.get(service), String.valueOf(services.get(service).port()));
        if (!r.ok() || r.output().isBlank()) {
            return -1;
        }
        String line = r.output().lines().filter(l -> l.startsWith("127.0.0.1:")).findFirst().orElse("");
        return line.isEmpty() ? -1 : Integer.parseInt(line.substring(line.lastIndexOf(':') + 1).strip());
    }

    @Override
    public void close() {
        for (String c : containers.values()) {
            try {
                Sh.run("rm", "-f", c);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (String n : networks) {
            try {
                Sh.run("network", "rm", n);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
