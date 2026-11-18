package com.jk.explore.security.securepipe;

import com.jk.explore.security.securepipe.Gate.Outcome;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        String gitleaks;
        List<String> gitleaksExtra = List.of();
        Path trivyCache;
        Consumer<Bundle> afterSigning = b -> {
        };
    }

    /**
     * Where it stopped (empty when it did not), how many gates passed, what the last gate's tool gave back, what it reported (findings, failed tests or the
     * check that refused) and the release it made.
     */
    record Result(String stoppedAt, int gatesPassed, Gate.Answer answer, List<String> found, Bundle bundle, String commit, String tree) {

        boolean deployable() {
            return stoppedAt.isEmpty();
        }

        Outcome outcome() {
            return answer.outcome();
        }
    }

    static final int GATES = 4;
    static final int SECRET_FOUND = 7;
    static final int VULNERABLE = 3;
    static final int TESTS_FAILED = 1;

    private Pipeline() {
    }

    static Result run(Git origin, Config c) throws IOException, InterruptedException {
        Git checkout = origin.cloneInto();
        checkout.must("checkout", "-q", c.revision);
        String commit = checkout.head();
        String tree = checkout.tree(commit);
        int passed = 0;

        Path secretReport = Scratch.file("gitleaks", ".json");
        List<String> scan = secretScanCommand(c, checkout.dir, secretReport);
        Gate.Answer secrets = Gate.run(checkout.dir, scan, SECRET_FOUND);
        if (!Gate.goesOn(secrets.outcome(), c.failClosed)) {
            List<String> found = Reports.gitleaks(secretReport);
            return stop("secret scan", passed, secrets, found, commit, tree);
        }
        passed++;

        Path bom = Scratch.file("bom", ".json");
        String buildGradle = Files.readString(checkout.dir.resolve("build.gradle"));
        List<Sbom.Component> dependencies = Sbom.components(buildGradle);
        Files.writeString(bom, Sbom.cycloneDx(dependencies));
        Path trivyReport = Scratch.file("trivy", ".json");
        Gate.Answer vulnerable = Gate.run(checkout.dir, trivyCommand(c, bom, trivyReport), VULNERABLE);
        if (!Gate.goesOn(vulnerable.outcome(), c.failClosed)) {
            return stop("dependency check", passed, vulnerable, Reports.trivy(trivyReport), commit, tree);
        }
        passed++;

        Gate.Answer tests = Gate.run(checkout.dir, Gradle.command(Gradle.wrapper(), "test"), TESTS_FAILED);
        if (!Gate.goesOn(tests.outcome(), c.failClosed)) {
            return stop("unit tests", passed, tests, Reports.junit(checkout.dir.resolve("build/test-results/test")), commit, tree);
        }
        passed++;

        Bundle bundle = new Bundle(Scratch.dir("release"));
        Files.copy(Gradle.jar(checkout.dir), bundle.jar());
        Provenance.Facts facts = new Provenance.Facts(Bundle.JAR, Digest.sha256(bundle.jar()), commit, tree, dependencies,
                Digest.sha256(buildGradle.getBytes(StandardCharsets.UTF_8)));
        Files.writeString(bundle.provenance(), Provenance.create(facts));
        if (c.sign) {
            Cosign.sign(c.signingKeys, bundle.jar(), bundle.jarSignature());
            Cosign.sign(c.signingKeys, bundle.provenance(), bundle.provenanceSignature());
        }
        c.afterSigning.accept(bundle);

        Verifier.Verdict verdict = Verifier.verify(bundle, c.trustedPublicKey, new Verifier.Policy(Provenance.BUILDER, commit, tree), new Cosign());
        if (!verdict.ok()) {
            return new Result("signature check", passed, new Gate.Answer(Outcome.FAIL, 1), List.of(verdict.failed()), bundle, commit, tree);
        }
        passed++;
        return new Result("", passed, new Gate.Answer(Outcome.PASS, 0), List.of(), bundle, commit, tree);
    }

    private static Result stop(String gate, int passed, Gate.Answer answer, List<String> found, String commit, String tree) {
        return new Result(gate, passed, answer, answer.outcome() == Outcome.FAIL ? found : List.of(), null, commit, tree);
    }

    static List<String> secretScanCommand(Config c, Path checkout, Path report) {
        List<String> command = new ArrayList<>(List.of(c.gitleaks, "git", "--no-banner", "-l", "fatal", "--redact", "--exit-code", String.valueOf(SECRET_FOUND),
                "--report-format", "json", "--report-path", report.toString()));
        command.addAll(c.gitleaksExtra);
        command.add(checkout.toString());
        return command;
    }

    static List<String> trivyCommand(Config c, Path bom, Path report) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(Tools.trivy(), "sbom", "--quiet", "--skip-db-update", "--severity", "HIGH,CRITICAL", "--exit-code",
                String.valueOf(VULNERABLE), "--format", "json", "--output", report.toString()));
        if (c.trivyCache != null) {
            command.addAll(List.of("--cache-dir", c.trivyCache.toString()));
        }
        command.add(bom.toString());
        return command;
    }
}
