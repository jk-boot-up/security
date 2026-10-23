package com.jk.explore.security.securepipe;

import com.jk.explore.security.securepipe.Gate.Outcome;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** The stages of a release, driven from Java: checkout, secret scan, dependency check, unit tests, build, provenance, signing and a signature check. */
final class Pipeline {

    /** Settings of one run. The defaults are the safe ones. */
    static final class Config {
        String revision = "HEAD";
        boolean failClosed = true;
        boolean sign = true;
        Cosign.Keys signingKeys;
        Path trustedPublicKey;
        String gitleaks = "gitleaks";
        List<String> gitleaksExtra = List.of();
        Path trivyCache;
        Consumer<Bundle> afterSigning = b -> {
        };
    }

    /** Where it stopped (empty when it did not), how many gates passed and the release it made. */
    record Result(String stoppedAt, int gatesPassed, Outcome outcome, Bundle bundle, String commit, String tree) {

        boolean deployable() {
            return stoppedAt.isEmpty();
        }
    }

    static final int GATES = 4;

    private Pipeline() {
    }

    static Result run(Git origin, Config c) throws IOException, InterruptedException {
        Git checkout = origin.cloneInto();
        checkout.must("checkout", "-q", c.revision);
        String commit = checkout.head();
        String tree = checkout.tree(commit);
        int passed = 0;

        Outcome secrets = Gate.tool(checkout.dir, secretScanCommand(c, checkout.dir), 7);
        if (!Gate.goesOn(secrets, c.failClosed)) {
            return stop("secret scan", passed, secrets, commit, tree);
        }
        passed++;

        Path bom = Scratch.file("bom", ".json");
        String buildGradle = Files.readString(checkout.dir.resolve("build.gradle"));
        List<Sbom.Component> dependencies = Sbom.components(buildGradle);
        Files.writeString(bom, Sbom.cycloneDx(dependencies));
        Outcome vulnerable = Gate.tool(checkout.dir, trivyCommand(c, bom), 3);
        if (!Gate.goesOn(vulnerable, c.failClosed)) {
            return stop("dependency check", passed, vulnerable, commit, tree);
        }
        passed++;

        Outcome tests = Gate.tool(checkout.dir, Gradle.command(Gradle.wrapper(), "test"), 1);
        if (!Gate.goesOn(tests, c.failClosed)) {
            return stop("unit tests", passed, tests, commit, tree);
        }
        passed++;

        Bundle bundle = new Bundle(Scratch.dir("release"));
        Files.copy(Gradle.jar(checkout.dir), bundle.jar());
        Provenance.Facts facts = new Provenance.Facts(Bundle.JAR, Digest.sha256(bundle.jar()), commit, tree, dependencies, Digest.sha256(buildGradle.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Files.writeString(bundle.provenance(), Provenance.create(facts));
        if (c.sign) {
            Cosign.sign(c.signingKeys, bundle.jar(), bundle.jarSignature());
            Cosign.sign(c.signingKeys, bundle.provenance(), bundle.provenanceSignature());
        }
        c.afterSigning.accept(bundle);

        Verifier.Verdict verdict = Verifier.verify(bundle, c.trustedPublicKey, new Verifier.Policy(Provenance.BUILDER, commit, tree), new Cosign());
        if (!verdict.ok()) {
            return new Result("signature check", passed, Outcome.FAIL, bundle, commit, tree);
        }
        passed++;
        return new Result("", passed, Outcome.PASS, bundle, commit, tree);
    }

    private static Result stop(String gate, int passed, Outcome outcome, String commit, String tree) {
        return new Result(gate, passed, outcome, null, commit, tree);
    }

    static List<String> secretScanCommand(Config c, Path checkout) {
        List<String> command = new java.util.ArrayList<>(List.of(c.gitleaks, "git", "--no-banner", "-l", "fatal", "--redact", "--exit-code", "7"));
        command.addAll(c.gitleaksExtra);
        command.add(checkout.toString());
        return command;
    }

    static List<String> trivyCommand(Config c, Path bom) {
        List<String> command = new java.util.ArrayList<>(List.of("trivy", "sbom", "--quiet", "--skip-db-update", "--severity", "HIGH,CRITICAL", "--exit-code", "3"));
        if (c.trivyCache != null) {
            command.addAll(List.of("--cache-dir", c.trivyCache.toString()));
        }
        command.add(bom.toString());
        return command;
    }
}
