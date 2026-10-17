package com.jk.explore.security.leastprivilegetf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Independent checks: OpenTofu (or Terraform) applies the real Terraform code to a local AWS emulator (LocalStack community image in
 * Docker), then the policies are read back from the emulator and compared with the fixtures the Java evaluator uses.
 * Run with ./gradlew integrationTest. Everything is skipped when Docker or tofu/terraform is missing, and the container is always removed.
 */
@Tag("terraform")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TerraformInteropTest {

    static final String IMAGE = "localstack/localstack:4.14.0";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final String container = "lptf-" + UUID.randomUUID().toString().substring(0, 8);
    private Path work;
    private String tofu;
    private String endpoint;
    private Map<String, String> environment;
    private JsonNode state;

    private static String find(String... names) {
        for (String name : names) {
            for (String dir : List.of("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin")) {
                if (Files.isExecutable(Path.of(dir, name))) {
                    return dir + "/" + name;
                }
            }
        }
        return null;
    }

    private record Result(int code, String output) {
    }

    private static Result run(Path dir, Map<String, String> env, String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).directory(dir == null ? null : dir.toFile()).redirectErrorStream(true);
        builder.redirectInput(ProcessBuilder.Redirect.from(new java.io.File("/dev/null")));
        if (env != null) {
            builder.environment().putAll(env);
        }
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        if (!process.waitFor(900, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("timed out: " + String.join(" ", command));
        }
        return new Result(process.exitValue(), output);
    }

    private Result tofu(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(tofu);
        command.addAll(List.of(args));
        return run(work, environment, command.toArray(String[]::new));
    }

    @BeforeAll
    void startEmulatorAndApply() throws Exception {
        tofu = find("tofu", "terraform");
        String docker = find("docker");
        Assumptions.assumeTrue(tofu != null && docker != null, "needs docker and tofu or terraform on the PATH");
        Assumptions.assumeTrue(run(null, null, docker, "info").code() == 0, "docker is not running");
        Result started = run(null, null, docker, "run", "-d", "--rm", "--name", container, "-p", "127.0.0.1:0:4566",
                "-e", "SERVICES=iam,sts", IMAGE);
        Assumptions.assumeTrue(started.code() == 0, "could not start " + IMAGE + ": " + started.output());
        String mapping = run(null, null, docker, "port", container, "4566/tcp").output().lines().findFirst().orElseThrow();
        endpoint = "http://127.0.0.1:" + mapping.substring(mapping.lastIndexOf(':') + 1).trim();
        waitUntilIamIsUp();

        work = Files.createTempDirectory("lptf-work");
        Files.copy(Path.of("infra", "terraform", "main.tf"), work.resolve("main.tf"));
        Path plugins = Path.of(System.getProperty("user.home"), ".cache", "lp-terraform-plugins");
        Files.createDirectories(plugins);
        environment = Map.of("TF_PLUGIN_CACHE_DIR", plugins.toString(), "TF_IN_AUTOMATION", "1", "NO_COLOR", "1");
        Result init = tofu("init", "-input=false");
        assertEquals(0, init.code(), init.output());
        Result apply = tofu("apply", "-auto-approve", "-input=false", "-var", "endpoint=" + endpoint);
        assertEquals(0, apply.code(), apply.output());
        state = MAPPER.readTree(tofu("show", "-json").output());
    }

    private void waitUntilIamIsUp() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        for (int i = 0; i < 90; i++) {
            try {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(endpoint + "/_localstack/health")).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 && (response.body().contains("\"iam\": \"running\"") || response.body().contains("\"iam\": \"available\""))) {
                    return;
                }
            } catch (IOException e) {
                // not up yet
            }
            Thread.sleep(2000);
        }
        throw new IllegalStateException("the emulator did not start");
    }

    @AfterAll
    void removeEverything() throws Exception {
        String docker = find("docker");
        if (docker != null) {
            run(null, null, docker, "rm", "-f", container);
        }
        if (work != null) {
            try (var files = Files.walk(work)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    void theConfigurationIsValidAndFormatted() throws Exception {
        assertEquals(0, tofu("validate").code());
        Result format = tofu("fmt", "-check", "-diff");
        assertEquals(0, format.code(), format.output());
    }

    @Test
    void policiesAfterApplyEqualTheFixtures() throws Exception {
        JsonNode documents = MAPPER.readTree(tofu("output", "-json", "documents").output());
        for (String name : PolicyNames.ALL) {
            assertEquals(MAPPER.readTree(Fixtures.json(name)), MAPPER.readTree(documents.get(name).asString()), name);
        }
    }

    @Test
    void theEmulatorStoresTheSamePoliciesTheJavaCodeEvaluates() throws Exception {
        int seen = 0;
        for (JsonNode resource : state.get("values").get("root_module").get("resources")) {
            if (!resource.get("type").asString().equals("aws_iam_policy")) {
                continue;
            }
            String key = resource.get("index").asString();
            JsonNode stored = MAPPER.readTree(resource.get("values").get("policy").asString());
            assertEquals(MAPPER.readTree(Fixtures.json(key)), stored, key);
            PolicyDocument fromState = PolicyDocument.parse(key, resource.get("values").get("policy").asString());
            assertEquals(Catalogue.blastRadius(Evaluator.of(Fixtures.policy(key))), Catalogue.blastRadius(Evaluator.of(fromState)), key);
            seen++;
        }
        assertEquals(5, seen);
    }

    @Test
    void theBoundaryIsAttachedToTheUser() {
        String boundaryArn = null;
        String boundaryOfUser = null;
        for (JsonNode resource : state.get("values").get("root_module").get("resources")) {
            String type = resource.get("type").asString();
            if (type.equals("aws_iam_policy") && resource.get("index").asString().equals("boundary")) {
                boundaryArn = resource.get("values").get("arn").asString();
            }
            if (type.equals("aws_iam_user")) {
                boundaryOfUser = resource.get("values").get("permissions_boundary").asString();
            }
        }
        assertTrue(boundaryArn != null && boundaryArn.endsWith("policy/shop-boundary"), "boundary arn " + boundaryArn);
        assertEquals(boundaryArn, boundaryOfUser);
    }

    @Test
    void aSecondPlanFindsNothingToChange() throws Exception {
        Result plan = tofu("plan", "-detailed-exitcode", "-input=false", "-var", "endpoint=" + endpoint);
        assertEquals(0, plan.code(), plan.output());
    }
}
