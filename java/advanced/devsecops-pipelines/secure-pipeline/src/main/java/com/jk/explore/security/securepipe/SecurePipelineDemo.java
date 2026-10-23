package com.jk.explore.security.securepipe;

import com.jk.explore.security.securepipe.BankLib.Flavour;
import com.jk.explore.security.securepipe.Gate.Outcome;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * A release pipeline for a bank's Gradle library, driven from Java: reproducible builds, signatures and provenance, gates that fail closed, and a deploy
 * step that accepts only what verifies. The tools are real (gradle, gitleaks, trivy, cosign, git); no Gitea or Woodpecker server is used.
 */
public final class SecurePipelineDemo {

    private final StringBuilder out = new StringBuilder();
    private Pipeline.Result previous;

    private SecurePipelineDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        SecurePipelineDemo d = new SecurePipelineDemo();
        try {
            d.demo();
        } finally {
            Scratch.cleanUp();
        }
        return d.out.toString();
    }

    private void line(String text) {
        out.append(text).append('\n');
    }

    private static String yn(boolean b) {
        return b ? "yes" : "no";
    }

    private void demo() throws Exception {
        Cosign.Keys trusted = Cosign.generate();
        Cosign.Keys attacker = Cosign.generate();
        Git clean = BankLib.origin(Flavour.CLEAN);

        reproducible();

        Pipeline.Config good = config(trusted, trusted);
        Pipeline.Result release = Pipeline.run(clean, good);
        signing(release, trusted, attacker);
        provenance(release, trusted, clean);
        gates(clean, trusted, attacker, release);
        failClosed(trusted);
        deploy(clean, release, trusted, attacker);

        line("--- the bill");
        line("the pipeline here is a sequence of stages driven from Java, with the tools a Gitea and Woodpecker setup would call");
        line("a signature says who built a file, provenance says from what, and neither says the source was good");
        line("a gate that cannot answer must be a gate that says no");
        line("every key above lived in a temporary directory");
        Cosign.destroy(trusted);
        Cosign.destroy(attacker);
        line("key files left on disk after the run: " + yn(Files.exists(trusted.privateKey()) || Files.exists(attacker.privateKey())));
    }

    private static Pipeline.Config config(Cosign.Keys signing, Cosign.Keys trusted) {
        Pipeline.Config c = new Pipeline.Config();
        c.signingKeys = signing;
        c.trustedPublicKey = trusted.publicKey();
        return c;
    }

    private void reproducible() throws Exception {
        line("--- the same commit built twice, in two clean checkouts in two directories");
        Reproducibility.Comparison fixed = Reproducibility.compare(BankLib.origin(Flavour.CLEAN));
        line("preserveFileTimestamps = false, reproducibleFileOrder = true: jars identical: " + yn(fixed.identical()));
        Reproducibility.Comparison times = Reproducibility.compare(BankLib.origin(Flavour.FILE_TIMES));
        line("preserveFileTimestamps = true, reproducibleFileOrder = false: jars identical: " + yn(times.identical()) + ", same entries: " + yn(times.sameEntries()) + ", entries with other content: "
                + times.differentContent().size() + ", file times differ: " + yn(times.differentTimes()));
        Reproducibility.Comparison path = Reproducibility.compare(BankLib.origin(Flavour.BUILD_PATH));
        line("build path written into the jar: jars identical: " + yn(path.identical()) + ", entries with other content: " + path.differentContent().size()
                + " (" + String.join(", ", path.differentContent()) + "), file times differ: " + yn(path.differentTimes()));
        line("Gradle 9 archives are reproducible by default, so the first case is also what an unset build does"); 
        line("the fix: preserveFileTimestamps = false, reproducibleFileOrder = true, and nothing about the machine in the jar");
    }

    private void signing(Pipeline.Result release, Cosign.Keys trusted, Cosign.Keys attacker) throws Exception {
        line("--- signing with cosign (a key pair made for this run)");
        Bundle b = release.bundle();
        Cosign cosign = new Cosign();
        line("the release passed all gates: " + yn(release.deployable()));
        line("jar signature verifies with the public key: " + yn(cosign.verify(trusted.publicKey(), b.jar(), b.jarSignature())));
        line("provenance signature verifies with the public key: " + yn(cosign.verify(trusted.publicKey(), b.provenance(), b.provenanceSignature())));
        Path changed = Scratch.file("jar", ".jar");
        byte[] bytes = Files.readAllBytes(b.jar());
        bytes[bytes.length / 2] ^= 1;
        Files.write(changed, bytes);
        line("the jar with one bit changed verifies: " + yn(cosign.verify(trusted.publicKey(), changed, b.jarSignature())));
        line("the signature checked with the public key of another key pair verifies: " + yn(cosign.verify(attacker.publicKey(), b.jar(), b.jarSignature())));
    }

    private void provenance(Pipeline.Result release, Cosign.Keys trusted, Git origin) throws Exception {
        line("--- provenance (an in-toto statement with a SLSA v1 predicate)");
        Bundle b = release.bundle();
        JsonNode p = Provenance.read(b.provenance());
        line("builder id: " + Provenance.builderId(p));
        line("build command: " + Provenance.command(p));
        line("inputs recorded: " + Provenance.inputs(p) + " (source commit and tree, build.gradle, 1 dependency)");
        line("subject digest equals the SHA-256 of the jar: " + yn(Provenance.subjectDigest(p).equals(Digest.sha256(b.jar()))));
        line("source commit equals the commit that was checked out: " + yn(Provenance.sourceCommit(p).equals(release.commit())));
        line("source tree equals the tree of that commit: " + yn(Provenance.sourceTree(p).equals(origin.tree(release.commit()))));
        Pipeline.Result older = previous(origin, trusted);
        JsonNode other = Provenance.read(older.bundle().provenance());
        line("the provenance of the previous release names this commit: " + yn(Provenance.sourceCommit(other).equals(release.commit())));
        Path edited = Scratch.file("provenance", ".json");
        Files.writeString(edited, Files.readString(b.provenance()).replace(release.commit(), "0".repeat(release.commit().length())));
        line("provenance edited to name another commit still verifies: " + yn(new Cosign().verify(trusted.publicKey(), edited, b.provenanceSignature())));
    }

    /** The release before this one, made once. */
    private Pipeline.Result previous(Git origin, Cosign.Keys trusted) throws Exception {
        if (previous == null) {
            previous = Pipeline.run(origin, olderConfig(trusted));
        }
        return previous;
    }

    private static Pipeline.Config olderConfig(Cosign.Keys trusted) {
        Pipeline.Config c = config(trusted, trusted);
        c.revision = "HEAD~1";
        return c;
    }

    /** One planted failure: what it is, the gate that must stop it, and the run that was made. */
    private record Case(String name, String gate, Pipeline.Result result) {
    }

    private void gates(Git clean, Cosign.Keys trusted, Cosign.Keys attacker, Pipeline.Result release) throws Exception {
        line("--- a pipeline with " + Pipeline.GATES + " gates (secret scan, dependency check, unit tests, signature check)");
        line("clean change: " + release.gatesPassed() + " of " + Pipeline.GATES + " gates passed, deployable: " + yn(release.deployable()));
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("failing test", "unit tests", Pipeline.run(BankLib.origin(Flavour.FAILING_TEST), config(trusted, trusted))));
        cases.add(new Case("vulnerable dependency", "dependency check", Pipeline.run(BankLib.origin(Flavour.VULNERABLE_DEPENDENCY), config(trusted, trusted))));
        cases.add(new Case("committed secret", "secret scan", Pipeline.run(BankLib.origin(Flavour.SECRET), config(trusted, trusted))));
        Pipeline.Config unsigned = config(trusted, trusted);
        unsigned.sign = false;
        cases.add(new Case("unsigned artifact", "signature check", Pipeline.run(clean, unsigned)));
        Pipeline.Config altered = config(trusted, trusted);
        altered.afterSigning = bundle -> {
            try {
                byte[] bytes = Files.readAllBytes(bundle.jar());
                bytes[bytes.length / 2] ^= 1;
                Files.write(bundle.jar(), bytes);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
        cases.add(new Case("jar altered after signing", "signature check", Pipeline.run(clean, altered)));
        cases.add(new Case("signed with an unknown key", "signature check", Pipeline.run(clean, config(attacker, trusted))));
        int stopped = 0;
        int rightGate = 0;
        for (Case c : cases) {
            boolean stop = !c.result().deployable();
            stopped += stop ? 1 : 0;
            rightGate += c.gate().equals(c.result().stoppedAt()) ? 1 : 0;
            line("planted " + c.name() + ": " + (stop ? "stopped at " + c.result().stoppedAt() : "LET THROUGH") + ", gates passed before: " + c.result().gatesPassed());
        }
        line("planted failures: " + cases.size() + ", stopped: " + stopped + ", let through: " + (cases.size() - stopped));
        line("each one stopped at the gate that owns the flaw: " + yn(rightGate == cases.size()));
    }

    /** A run with a broken tool, and what a pipeline that fails closed and one that fails open do with it. */
    private record Broken(String what, String gate, Pipeline.Result closed, Pipeline.Result open) {
    }

    private void failClosed(Cosign.Keys trusted) throws Exception {
        line("--- a gate whose tool is missing or crashes");
        Git secret = BankLib.origin(Flavour.SECRET);
        Git vulnerable = BankLib.origin(Flavour.VULNERABLE_DEPENDENCY);
        Path badConfig = Scratch.file("gitleaks", ".toml");
        Files.writeString(badConfig, "this is [not = valid toml\n");
        Path emptyCache = Scratch.dir("trivycache");
        List<Broken> runs = new ArrayList<>();
        runs.add(broken("gitleaks is not installed (committed secret)", "secret scan", secret, trusted, c -> c.gitleaks = "gitleaks-not-installed"));
        runs.add(broken("gitleaks crashes on its configuration (committed secret)", "secret scan", secret, trusted, c -> c.gitleaksExtra = List.of("--config", badConfig.toString())));
        runs.add(broken("trivy has no vulnerability database (vulnerable dependency)", "dependency check", vulnerable, trusted, c -> c.trivyCache = emptyCache));
        int closed = 0;
        int open = 0;
        for (Broken r : runs) {
            boolean stopped = !r.closed().deployable() && r.gate().equals(r.closed().stoppedAt()) && r.closed().outcome() == Outcome.ERROR;
            closed += stopped ? 1 : 0;
            open += r.open().deployable() ? 1 : 0;
            line(r.what() + ": fails closed: " + (stopped ? "stopped at " + r.closed().stoppedAt() : "went on") + "; fails open: "
                    + (r.open().deployable() ? "released" : "stopped"));
        }
        line("broken tools: " + runs.size() + ", stopped by the pipeline that fails closed: " + closed + ", released by the pipeline that fails open: " + open);
    }

    private Broken broken(String what, String gate, Git origin, Cosign.Keys trusted, java.util.function.Consumer<Pipeline.Config> breakIt) throws Exception {
        Pipeline.Config closed = config(trusted, trusted);
        breakIt.accept(closed);
        Pipeline.Config open = config(trusted, trusted);
        breakIt.accept(open);
        open.failClosed = false;
        return new Broken(what, gate, Pipeline.run(origin, closed), Pipeline.run(origin, open));
    }

    private void deploy(Git origin, Pipeline.Result release, Cosign.Keys trusted, Cosign.Keys attacker) throws Exception {
        line("--- deploy accepts only a release that verifies (signature, provenance, digest, source)");
        Verifier.Policy policy = new Verifier.Policy(Provenance.BUILDER, release.commit(), release.tree());
        Deploy deploy = new Deploy(Scratch.dir("production"), trusted.publicKey(), policy, new Cosign());

        Verifier.Verdict genuine = deploy.deploy(release.bundle());
        line("the release the pipeline made: " + (genuine.ok() ? "accepted" : "refused at " + genuine.failed()));

        Bundle swapped = copy(release.bundle());
        byte[] bytes = Files.readAllBytes(swapped.jar());
        bytes[bytes.length / 2] ^= 1;
        Files.write(swapped.jar(), bytes);
        Verifier.Verdict v1 = deploy.deploy(swapped);
        line("forged 1, jar swapped, genuine signatures kept: " + (v1.ok() ? "ACCEPTED" : "refused at " + v1.failed()));

        Bundle own = new Bundle(Scratch.dir("forged"));
        Files.write(own.jar(), (Files.readString(release.bundle().jar(), StandardCharsets.ISO_8859_1) + "backdoor").getBytes(StandardCharsets.ISO_8859_1));
        Files.writeString(own.provenance(), Provenance.create(new Provenance.Facts(Bundle.JAR, Digest.sha256(own.jar()), release.commit(), release.tree(),
                List.of(), Digest.sha256(new byte[0]))));
        Cosign.sign(attacker, own.jar(), own.jarSignature());
        Cosign.sign(attacker, own.provenance(), own.provenanceSignature());
        Verifier.Verdict v2 = deploy.deploy(own);
        line("forged 2, own build with matching provenance, signed with its own key: " + (v2.ok() ? "ACCEPTED" : "refused at " + v2.failed()));

        Pipeline.Result older = previous(origin, trusted);
        Verifier.Verdict v3 = deploy.deploy(older.bundle());
        line("forged 3, the genuine previous release replayed: " + (v3.ok() ? "ACCEPTED" : "refused at " + v3.failed()));

        int refused = (v1.ok() ? 0 : 1) + (v2.ok() ? 0 : 1) + (v3.ok() ? 0 : 1);
        line("forged releases: 3, refused: " + refused + ", jars in production: " + deploy.deployed());
    }

    private static Bundle copy(Bundle from) throws IOException {
        Bundle to = new Bundle(Scratch.dir("copy"));
        Files.copy(from.jar(), to.jar(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(from.jarSignature(), to.jarSignature());
        Files.copy(from.provenance(), to.provenance());
        Files.copy(from.provenanceSignature(), to.provenanceSignature());
        return to;
    }
}
