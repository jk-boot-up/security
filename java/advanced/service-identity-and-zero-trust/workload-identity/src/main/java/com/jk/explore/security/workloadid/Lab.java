package com.jk.explore.security.workloadid;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A real SPIRE server and agent in one Linux container, plus the Java workloads that run beside them as different operating system users.
 * SPIRE publishes no macOS build, so the container is the way to run it here: the binaries are the official linux arm64 release
 * in build/spire, the workloads run on a Java 25 image, and everything is removed when the lab is closed.
 */
final class Lab implements AutoCloseable {

    static final String SPIRE_VERSION = "1.15.5";
    static final String IMAGE = "eclipse-temurin:25-jre";
    static final String DOMAIN = "bank.example";
    static final String NODE = "spiffe://" + DOMAIN + "/node/lab";
    static final int SVID_TTL_SECONDS = 20;
    /** The operating system user each workload runs as. SPIRE tells the workloads apart by this number. */
    static final Map<String, Integer> UIDS = Map.of("ledger", 2001, "payments", 2002, "reporting", 2003, "attacker", 2004);

    private static final String SERVER = "/spire/bin/spire-server";
    private static final String AGENT = "/spire/bin/spire-agent";
    private static final String SERVER_SOCKET = "/tmp/spire-server.sock";
    private static final String AGENT_SOCKET = "/tmp/spire-agent/public/api.sock";

    private final String container = "workloadid-lab-" + ProcessHandle.current().pid();
    private final Path work;
    private final List<String> classpath = new ArrayList<>();

    private Lab(Path work) {
        this.work = work;
    }

    Path work() {
        return work;
    }

    /** Starts the container, the SPIRE server, the agent and the three registration entries. */
    static Lab start() throws IOException, InterruptedException {
        Path build = Path.of("build").toAbsolutePath();
        Path spire = ensureSpire(build);
        if (!Sh.run("docker", "image", "inspect", IMAGE).ok()) {
            Sh.must(List.of("docker", "pull", IMAGE));
        }
        Path work = build.resolve("lab");
        deleteTree(work);
        Files.createDirectories(work);
        Lab lab = new Lab(work);
        lab.stageApplication();
        Files.writeString(work.resolve("server.conf"), """
                server {
                  bind_address = "127.0.0.1"
                  bind_port = "8081"
                  socket_path = "%s"
                  trust_domain = "%s"
                  data_dir = "/tmp/spire-server-data"
                  log_level = "INFO"
                  default_x509_svid_ttl = "%ds"
                }
                plugins {
                  DataStore "sql" { plugin_data { database_type = "sqlite3" connection_string = "/tmp/spire-server-data/datastore.sqlite3" } }
                  NodeAttestor "join_token" { plugin_data {} }
                  KeyManager "memory" { plugin_data = {} }
                }
                """.formatted(SERVER_SOCKET, DOMAIN, SVID_TTL_SECONDS));
        Files.writeString(work.resolve("agent.conf"), """
                agent {
                  data_dir = "/tmp/spire-agent-data"
                  log_level = "INFO"
                  server_address = "127.0.0.1"
                  server_port = "8081"
                  socket_path = "%s"
                  trust_bundle_path = "/tmp/bundle.pem"
                  trust_domain = "%s"
                }
                plugins {
                  NodeAttestor "join_token" { plugin_data {} }
                  KeyManager "memory" { plugin_data {} }
                  WorkloadAttestor "unix" { plugin_data {} }
                }
                """.formatted(AGENT_SOCKET, DOMAIN));
        try {
            Sh.must(List.of("docker", "run", "-d", "--name", lab.container,
                    "-v", spire + ":/spire:ro", "-v", work + ":/work", IMAGE, "sleep", "infinity"));
            lab.sh("/spire/bin/spire-server run -config /work/server.conf > /work/server.log 2>&1", true);
            lab.until("the SPIRE server", 30, () -> lab.root("sh", "-c", SERVER + " healthcheck -socketPath " + SERVER_SOCKET).ok());
            lab.root("sh", "-c", SERVER + " bundle show -socketPath " + SERVER_SOCKET + " > /tmp/bundle.pem && cp /tmp/bundle.pem /work/bundle.pem");
            // the join token is the one secret in this lab: it lives in the agent's environment and nowhere else
            String token = lab.server("token", "generate", "-spiffeID", NODE).replace("Token:", "").trim();
            Sh.must(List.of("docker", "exec", "-d", "-e", "JOIN=" + token, lab.container, "sh", "-c",
                    AGENT + " run -config /work/agent.conf -joinToken \"$JOIN\" > /work/agent.log 2>&1"));
            lab.until("the SPIRE agent", 30, () -> lab.root("sh", "-c", AGENT + " healthcheck -socketPath " + AGENT_SOCKET).ok());
            for (String workload : List.of("ledger", "payments", "reporting")) {
                lab.createEntry(workload);
            }
            for (String workload : List.of("ledger", "payments", "reporting")) {
                lab.until("the entry of " + workload, 40, () -> lab.agentCli(UIDS.get(workload), "api", "fetch", "x509").ok());
            }
        } catch (RuntimeException | IOException | InterruptedException e) {
            lab.close();
            throw e;
        }
        return lab;
    }

    /** The registration entry: this SPIFFE ID goes to a process the agent sees running as this user. The workload's own claims are never consulted. */
    void createEntry(String workload) throws IOException, InterruptedException {
        server("entry", "create", "-parentID", NODE, "-spiffeID", "spiffe://" + DOMAIN + "/" + workload,
                "-selector", "unix:uid:" + UIDS.get(workload), "-x509SVIDTTL", Integer.toString(SVID_TTL_SECONDS));
    }

    void deleteEntry(String workload) throws IOException, InterruptedException {
        String shown = server("entry", "show", "-spiffeID", "spiffe://" + DOMAIN + "/" + workload, "-output", "json");
        Matcher id = Pattern.compile("\"id\":\\s*\"([^\"]+)\"").matcher(shown);
        while (id.find()) {
            server("entry", "delete", "-entryID", id.group(1));
        }
    }

    /** How many workload entries the server holds (the node's own entry is not one of them). */
    int entryCount() throws IOException, InterruptedException {
        Matcher m = Pattern.compile("Found (\\d+) entr").matcher(server("entry", "show", "-parentID", NODE));
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    String server(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("docker", "exec", container, SERVER, args[0], args[1]));
        command.addAll(List.of(args).subList(2, args.length));
        command.addAll(List.of("-socketPath", SERVER_SOCKET));
        return Sh.must(command);
    }

    /** Runs the spire-agent command line as a workload user, for example the one that fetches an SVID. */
    Sh.Result agentCli(int uid, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(AGENT));
        command.addAll(List.of(args));
        command.addAll(List.of("-socketPath", AGENT_SOCKET));
        return as(uid, command.toArray(new String[0]));
    }

    Sh.Result root(String... command) throws IOException, InterruptedException {
        List<String> all = new ArrayList<>(List.of("docker", "exec", container));
        all.addAll(List.of(command));
        return Sh.run(all);
    }

    Sh.Result as(int uid, String... command) throws IOException, InterruptedException {
        List<String> all = new ArrayList<>(List.of("docker", "exec", "-u", Integer.toString(uid), container));
        all.addAll(List.of(command));
        return Sh.run(all);
    }

    /** Runs the Java workload as a user and returns what follows RESULT on its last output line. */
    String workload(int uid, String... args) throws IOException, InterruptedException {
        return workloadClaiming(uid, null, args);
    }

    /** Like workload, but the process also carries a claim about who it is in its environment (SPIRE does not read it). */
    String workloadClaiming(int uid, String claim, String... args) throws IOException, InterruptedException {
        return workloadClaiming(uid, claim, 20, args);
    }

    /** A workload that is expected to get no SVID: it waits only a few seconds for one, where a workload that should have one waits 20. */
    String workloadWithoutEntry(int uid, String claim, String... args) throws IOException, InterruptedException {
        return workloadClaiming(uid, claim, 4, args);
    }

    private String workloadClaiming(int uid, String claim, int waitSeconds, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        if (claim != null) {
            command.addAll(List.of("env", "CLAIMED_ID=" + claim));
        }
        command.addAll(List.of("java", "-Dsvid.wait=" + waitSeconds));
        command.addAll(List.of(javaCommand(args)).subList(1, javaCommand(args).length));
        Sh.Result r = as(uid, command.toArray(new String[0]));
        String last = "";
        for (String line : r.output().split("\n")) {
            if (line.startsWith("RESULT ")) {
                last = line.substring("RESULT ".length()).trim();
            }
        }
        if (last.isEmpty()) {
            throw new IllegalStateException("no RESULT line from " + List.of(args) + ": " + r.output());
        }
        return last;
    }

    /** Starts a long-running Java workload (a server) in the background as a user. */
    void spawn(int uid, String name, String... args) throws IOException, InterruptedException {
        String[] java = javaCommand(args);
        Sh.must(List.of("docker", "exec", "-d", "-u", Integer.toString(uid), container, "sh", "-c",
                String.join(" ", java) + " > /work/" + name + ".log 2>&1"));
        until("the server " + name, 90, () -> Files.exists(work.resolve("port-" + name)));
    }

    /** The lines the ledger wrote about the requests that reached its own code. */
    List<String> audit(String name) throws IOException {
        Path file = work.resolve("audit-" + name + ".log");
        return Files.exists(file) ? Files.readAllLines(file) : List.of();
    }

    private String[] javaCommand(String... args) {
        List<String> command = new ArrayList<>(List.of("java", "-XX:TieredStopAtLevel=1", "-Xshare:auto", "-cp", String.join(":", classpath),
                Workload.class.getName()));
        command.addAll(List.of(args));
        return command.toArray(new String[0]);
    }

    /** Copies the program's own classes and libraries to a folder the container can see. */
    private void stageApplication() throws IOException {
        Path app = work.resolve("app");
        Files.createDirectories(app);
        int n = 0;
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            Path source = Path.of(entry);
            if (Files.isDirectory(source)) {
                Path target = app.resolve("classes" + n++);
                try (Stream<Path> files = Files.walk(source)) {
                    for (Path file : (Iterable<Path>) files::iterator) {
                        Path to = target.resolve(source.relativize(file).toString());
                        if (Files.isDirectory(file)) {
                            Files.createDirectories(to);
                        } else {
                            Files.copy(file, to);
                        }
                    }
                }
                classpath.add("/work/app/" + target.getFileName());
            } else if (Files.isRegularFile(source)) {
                Files.copy(source, app.resolve(source.getFileName().toString()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                classpath.add("/work/app/" + source.getFileName());
            }
        }
    }

    private void sh(String script, boolean detached) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("docker", "exec"));
        if (detached) {
            command.add("-d");
        }
        command.addAll(List.of(container, "sh", "-c", script));
        Sh.must(command);
    }

    /** The official release for this machine's Linux architecture, downloaded once into build/spire. */
    private static Path ensureSpire(Path build) throws IOException, InterruptedException {
        Path spire = build.resolve("spire");
        if (Files.exists(spire.resolve("bin/spire-server"))) {
            return spire;
        }
        Path download = build.resolve("spire-download");
        deleteTree(download);
        Files.createDirectories(download);
        String archive = "spire-" + SPIRE_VERSION + "-linux-arm64-musl.tar.gz";
        Sh.must(List.of("curl", "-sSL", "-o", download.resolve(archive).toString(),
                "https://github.com/spiffe/spire/releases/download/v" + SPIRE_VERSION + "/" + archive));
        Sh.must(List.of("tar", "-xzf", download.resolve(archive).toString(), "-C", download.toString()));
        Files.move(download.resolve("spire-" + SPIRE_VERSION), spire);
        deleteTree(download);
        return spire;
    }

    private void until(String what, int seconds, Check check) throws IOException, InterruptedException {
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < end) {
            if (check.ok()) {
                return;
            }
            Thread.sleep(300);
        }
        throw new IllegalStateException("timed out waiting for " + what);
    }

    /** A condition that is polled. */
    @FunctionalInterface
    interface Check {

        boolean ok() throws IOException, InterruptedException;
    }

    static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> files = Files.walk(path)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    @Override
    public void close() {
        try {
            Sh.run("docker", "rm", "-f", container);
        } catch (IOException | InterruptedException e) {
            // nothing more can be done; the container name carries this process id
        }
    }
}
