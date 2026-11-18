package com.jk.explore.security.cosignsigning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** The cosign command, run as a program, with a key pair per signer kept in the lab's folder. */
final class Cosign {

    /** What cosign said: whether it succeeded, and its output (verification prints the claims it found as JSON). */
    record Result(boolean ok, String output) {

        /** "accepted" or "refused". */
        String word() {
            return ok ? "accepted" : "refused";
        }

        /** Why cosign refused, in the terms of the lesson; empty when it succeeded. */
        String why() {
            if (ok) {
                return "";
            }
            if (output.contains("missing or incorrect annotation")) {
                return "claims do not match the gate";
            }
            if (output.contains("do not match threshold")) {
                return "not signed by this key";
            }
            if (output.contains("no signatures found") || output.contains("no attestations found") || output.contains("no matching attestations")) {
                return "no signature for this digest";
            }
            return output.lines().filter(l -> l.startsWith("Error:")).findFirst().orElse("failed").replace("Error: ", "");
        }

        /** The word, and the reason when refused. */
        String reason() {
            return ok ? word() : word() + " (" + why() + ")";
        }
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final Path folder;

    Cosign(Path folder) {
        this.folder = folder;
    }

    /** Makes a key pair named {@code name} (name.key and name.pub). */
    void generateKeyPair(String name) throws IOException, InterruptedException {
        Path dir = Files.createDirectories(folder.resolve(name));
        run(dir, Lab.PASSPHRASE, List.of("cosign", "generate-key-pair"));
    }

    Path publicKey(String name) {
        return folder.resolve(name).resolve("cosign.pub");
    }

    Result sign(String name, String digestRef, Map<String, String> annotations) throws IOException, InterruptedException {
        return sign(name, Lab.PASSPHRASE, digestRef, annotations);
    }

    Result sign(String name, String passphrase, String digestRef, Map<String, String> annotations) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("cosign", "sign", "--key", folder.resolve(name).resolve("cosign.key").toString(), "--use-signing-config=false", "--tlog-upload=false", "--yes"));
        annotations.forEach((k, v) -> command.addAll(List.of("-a", k + "=" + v)));
        command.add(digestRef);
        return run(folder, passphrase, command);
    }

    /** Verifies with a public key, the transparency log left out because the lab is local, and the given annotations required. */
    Result verify(String keyName, String digestRef, Map<String, String> required) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("cosign", "verify",
                "--key", publicKey(keyName).toString(),
                "--insecure-ignore-tlog=true"));
        required.forEach((k, v) -> command.addAll(List.of("-a", k + "=" + v)));
        command.add(digestRef);
        return run(folder, null, command);
    }

    Result attest(String name, String digestRef, String type, Path predicate) throws IOException, InterruptedException {
        return run(folder, Lab.PASSPHRASE, List.of("cosign", "attest", "--key", folder.resolve(name).resolve("cosign.key").toString(), "--use-signing-config=false", "--tlog-upload=false", "--yes",
                "--type", type, "--predicate", predicate.toString(), digestRef));
    }

    /** Verifies attestations and returns the predicates of the ones that verified. */
    Result verifyAttestation(String keyName, String digestRef, String type) throws IOException, InterruptedException {
        return run(folder, null, List.of("cosign", "verify-attestation", "--key", publicKey(keyName).toString(), "--insecure-ignore-tlog=true", "--type", type, digestRef));
    }

    /** The predicate inside the first attestation line of a verification's output. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> predicateOf(String output) {
        String line = output.lines().filter(l -> l.startsWith("{")).findFirst().orElseThrow();
        Map<String, Object> envelope = JSON.readValue(line, new TypeReference<Map<String, Object>>() {
        });
        byte[] payload = java.util.Base64.getDecoder().decode(String.valueOf(envelope.get("payload")));
        Map<String, Object> statement = JSON.readValue(payload, new TypeReference<Map<String, Object>>() {
        });
        return (Map<String, Object>) statement.get("predicate");
    }

    /** The payloads that a successful {@code cosign verify} prints: the JSON array line of its output. */
    static List<SignedPayload> payloadsOf(String output) {
        String line = output.lines().filter(l -> l.startsWith("[")).findFirst().orElseThrow();
        List<Map<String, Object>> all = JSON.readValue(line, new TypeReference<List<Map<String, Object>>>() {
        });
        return all.stream().map(SignedPayload::of).toList();
    }

    private static Result run(Path dir, String passphrase, List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true);
        if (passphrase != null) {
            builder.environment().put("COSIGN_PASSWORD", passphrase);
        }
        Process p = builder.start();
        String out = new String(p.getInputStream().readAllBytes());
        boolean done = p.waitFor(120, TimeUnit.SECONDS);
        return new Result(done && p.exitValue() == 0, out);
    }
}
