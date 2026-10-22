package com.jk.explore.security.depconfusion;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** A Maven repository on a loopback port that remembers every path it was asked for. Plain http: only a lab does that. */
public final class Repo implements AutoCloseable {

    private final String name;
    private final HttpServer server;
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private final Map<String, TreeSet<String>> versions = new ConcurrentHashMap<>();
    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());

    public Repo(String name) throws IOException {
        this.name = name;
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.add(path);
            byte[] body = path.endsWith("maven-metadata.xml") ? metadata(path) : files.get(path);
            boolean head = exchange.getRequestMethod().equals("HEAD");
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
            } else if (head) {
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(body.length));
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String name() {
        return name;
    }

    /** Publishes group:artifact:version; the jar holds origin.txt saying who published it, so a build can show where a jar really came from. */
    void publish(String group, String artifact, String version, String origin) throws IOException {
        String base = "/" + group.replace('.', '/') + "/" + artifact + "/" + version + "/" + artifact + "-" + version;
        String pom = "<project><modelVersion>4.0.0</modelVersion><groupId>" + group + "</groupId><artifactId>" + artifact + "</artifactId><version>" + version + "</version></project>";
        files.put(base + ".pom", pom.getBytes(StandardCharsets.UTF_8));
        files.put(base + ".jar", jar(origin));
        versions.computeIfAbsent("/" + group.replace('.', '/') + "/" + artifact, k -> new TreeSet<>()).add(version);
    }

    private static byte[] jar(String origin) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            JarEntry entry = new JarEntry("origin.txt");
            entry.setTime(0);
            jar.putNextEntry(entry);
            jar.write(origin.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return bytes.toByteArray();
    }

    private byte[] metadata(String path) {
        TreeSet<String> found = versions.get(path.substring(0, path.length() - "/maven-metadata.xml".length()));
        if (found == null) {
            return null;
        }
        String[] parts = path.substring(1).split("/");
        StringBuilder xml = new StringBuilder("<metadata><groupId>" + String.join(".", java.util.Arrays.copyOf(parts, parts.length - 2)) + "</groupId><artifactId>" + parts[parts.length - 2] + "</artifactId><versioning><versions>");
        found.forEach(v -> xml.append("<version>").append(v).append("</version>"));
        return xml.append("</versions></versioning></metadata>").toString().getBytes(StandardCharsets.UTF_8);
    }

    /** The paths asked for so far, in order. */
    List<String> requests() {
        synchronized (requests) {
            return new ArrayList<>(requests);
        }
    }

    long requestsUnder(String groupPath) {
        return requests().stream().filter(p -> p.startsWith("/" + groupPath + "/")).count();
    }

    void clearRequests() {
        requests.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    Map<String, Integer> summary() {
        Map<String, Integer> out = new TreeMap<>();
        requests().forEach(p -> out.merge(p, 1, Integer::sum));
        return out;
    }
}
