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
import java.util.function.Consumer;
import tools.jackson.databind.JsonNode;

/**
 * A release pipeline for a bank's Gradle library, driven from Java: reproducible builds, signatures and provenance, gates that fail closed, and a deploy
 * step that accepts only what verifies. The tools are real, pinned releases (gitleaks, trivy, cosign) next to git and Gradle; no Gitea or Woodpecker
 * server is used. Keys, temporary paths and jar digests are never printed, so every run prints the same.
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

    private void line(String format, Object... args) {
        line(String.format(format, args));
    }

    private static String yn(boolean b) {
        return b ? "yes" : "no";
    }

    /** The first 12 characters of a commit, tree or digest: enough to tell them apart. */
    static String shortId(String id) {
        return id.substring(0, 12);
    }

    private void demo() throws Exception {
        Tools.trivyDatabase();
        Cosign.Keys trusted = Cosign.generate();
        Cosign.Keys attacker = Cosign.generate();
        Git clean = BankLib.origin(Flavour.CLEAN);

        reproducible();
        Pipeline.Result release = Pipeline.run(clean, config(trusted, trusted));
        signing(release, trusted, attacker);
        provenance(release, trusted, clean);
        gates(clean, trusted, attacker, release);
        failClosed(trusted);
        deploy(clean, release, trusted, attacker);

        Cosign.destroy(trusted);
        Cosign.destroy(attacker);
        line("  THE KEYS:");
        line("    both key pairs lived in a temporary directory");
        line("    key files left on disk after the run: " + yn(Files.exists(trusted.privateKey()) || Files.exists(attacker.privateKey())));
    }

    private static Pipeline.Config config(Cosign.Keys signing, Cosign.Keys trusted) throws IOException, InterruptedException {
        Pipeline.Config c = new Pipeline.Config();
        c.signingKeys = signing;
        c.trustedPublicKey = trusted.publicKey();
        c.gitleaks = Tools.gitleaks();
        return c;
    }

    // ------------------------------------------------------------------ ONE

    private void reproducible() throws Exception {
        line("ONE. The same commit, built twice.");
        Git fixedRepo = BankLib.origin(Flavour.CLEAN);
        line("  THE INPUT (bank-ledger, commit %s):", shortId(fixedRepo.head()));
        line("    two clean checkouts in two directories, 2 seconds apart");
        line("    each one built with gradlew jar, in 3 settings:");
        line("    fixed       preserveFileTimestamps false, order fixed");
        line("    file times  preserveFileTimestamps true, order from disk");
        line("    build path  fixed, and the build path in the jar");
        Reproducibility.Comparison fixed = Reproducibility.compare(fixedRepo);
        Reproducibility.Comparison times = Reproducibility.compare(BankLib.origin(Flavour.FILE_TIMES));
        Reproducibility.Comparison path = Reproducibility.compare(BankLib.origin(Flavour.BUILD_PATH));
        line("  THE RESULT, BUILD 1 AGAINST BUILD 2:");
        line("    setting     same SHA-256  entries  other bytes  file times");
        row("fixed", fixed);
        row("file times", times);
        row("build path", path);
        line("  THE FILE TIMES IN THE JARS:");
        line("    fixed: every entry has the time %s", fixed.classTime());
        line("    file times: each entry has the time of its own build");
        line("  THE BUILD PATH IN THE JAR:");
        for (int i = 0; i < path.differentContent().size(); i++) {
            line("    %s holds", path.differentContent().get(i));
            line("      build 1  %s", path.differingText().get(2 * i));
            line("      build 2  %s", path.differingText().get(2 * i + 1));
        }
        line("  THE FIX:");
        line("    fixed archive settings (Gradle 9's default),");
        line("    and nothing about the machine in the jar");
    }

    private void row(String name, Reproducibility.Comparison c) {
        line("    %-11s %-13s %-8s %-12s %s", name, yn(c.identical()), c.sameEntries() ? c.entries() + " = " + c.entries() : "differ",
                c.differentContent().size(), c.differentTimes() ? "differ" : "same");
    }

    // ------------------------------------------------------------------ TWO

    private void signing(Pipeline.Result release, Cosign.Keys trusted, Cosign.Keys attacker) throws Exception {
        line("TWO. Sign the jar, and check the signature.");
        Bundle b = release.bundle();
        line("  THE INPUT:");
        line("    bank-ledger.jar, built from commit %s", shortId(release.commit()));
        line("    the release passed all %d gates: %s", Pipeline.GATES, yn(release.deployable()));
        line("    a key pair made for this run: cosign.key, cosign.pub");
        line("    the key in cosign.pub: %s (the curve P-256)", SignatureSteps.describe(trusted.publicKey()));
        line("  HOW THE SIGNATURE IS MADE (cosign sign-blob):");
        line("    1. the SHA-256 of all the bytes of the jar");
        line("    2. ECDSA signs that digest with cosign.key");
        line("    3. bank-ledger.jar.sig holds the signature, in base64");
        Path changed = Scratch.file("jar", ".jar");
        byte[] bytes = Files.readAllBytes(b.jar());
        bytes[bytes.length / 2] ^= 1;
        Files.write(changed, bytes);
        Cosign cosign = new Cosign();
        SignatureSteps jdk = new SignatureSteps();
        line("  THE CHECK (cosign verify-blob, then the JDK alone):");
        line("    what is checked                    cosign  JDK");
        check("the jar as built", trusted.publicKey(), b.jar(), b.jarSignature(), cosign, jdk);
        check("the jar, one bit changed", trusted.publicKey(), changed, b.jarSignature(), cosign, jdk);
        check("the jar, checked with another key", attacker.publicKey(), b.jar(), b.jarSignature(), cosign, jdk);
        check("provenance.json as built", trusted.publicKey(), b.provenance(), b.provenanceSignature(), cosign, jdk);
    }

    private void check(String what, Path key, Path file, Path signature, Cosign cosign, SignatureSteps jdk) {
        line("    %-34s %-7s %s", what, yn(cosign.verify(key, file, signature)), yn(jdk.verify(key, file, signature)));
    }

    // ------------------------------------------------------------------ THREE

    private void provenance(Pipeline.Result release, Cosign.Keys trusted, Git origin) throws Exception {
        line("THREE. Say where it came from.");
        Bundle b = release.bundle();
        JsonNode p = Provenance.read(b.provenance());
        line("  THE STATEMENT (provenance.json):");
        line("    _type          %s", Provenance.type(p));
        line("    predicateType  %s", Provenance.predicateType(p));
        line("    subject        %s, its SHA-256", Provenance.subjectName(p));
        line("    builder id     %s", Provenance.builderId(p));
        line("    command        %s", Provenance.command(p));
        List<String> uris = Provenance.inputUris(p);
        line("  THE INPUTS IT RECORDS (%d):", Provenance.inputs(p));
        line("    the source      commit %s, tree %s", shortId(Provenance.sourceCommit(p)), shortId(Provenance.sourceTree(p)));
        line("    the build file  %s, sha256 %s", uris.get(1).replace("file:", ""), shortId(Provenance.buildFileDigest(p)));
        for (String dependency : uris.subList(2, uris.size())) {
            line("    a dependency    %s", dependency);
        }
        line("  THE CHECKS:");
        line("    subject = the SHA-256 of the jar             %s", yn(Provenance.subjectDigest(p).equals(Digest.sha256(b.jar()))));
        line("    commit = the commit that was checked out     %s", yn(Provenance.sourceCommit(p).equals(release.commit())));
        line("    tree = the tree of that commit               %s", yn(Provenance.sourceTree(p).equals(origin.tree(release.commit()))));
        Pipeline.Result older = previous(origin, trusted);
        JsonNode other = Provenance.read(older.bundle().provenance());
        Path edited = Scratch.file("provenance", ".json");
        Files.writeString(edited, Files.readString(b.provenance()).replace(release.commit(), "0".repeat(release.commit().length())));
        line("  TWO THAT DO NOT MATCH:");
        line("    the previous release names commit %s: %s", shortId(Provenance.sourceCommit(other)),
                Provenance.sourceCommit(other).equals(release.commit()) ? "this one" : "not this one");
        line("    the statement edited to name commit %s", shortId("0".repeat(40)));
        line("      its signature still verifies: %s", yn(new Cosign().verify(trusted.publicKey(), edited, b.provenanceSignature())));
    }

    /** The release before this one, made once. */
    private Pipeline.Result previous(Git origin, Cosign.Keys trusted) throws Exception {
        if (previous == null) {
            Pipeline.Config c = config(trusted, trusted);
            c.revision = "HEAD~1";
            previous = Pipeline.run(origin, c);
        }
        return previous;
    }

    // ------------------------------------------------------------------ FOUR

    /** One planted failure: a short name, what was planted, the gate that must stop it, and the run that was made. */
    private record Case(String name, String planted, String gate, Pipeline.Result result) {
    }

    private void gates(Git clean, Cosign.Keys trusted, Cosign.Keys attacker, Pipeline.Result release) throws Exception {
        line("FOUR. Four gates, and six planted flaws.");
        line("  THE GATES, IN ORDER (the tool, its exit code for found):");
        line("    1 secret scan       gitleaks git, exit %d", Pipeline.SECRET_FOUND);
        line("    2 dependency check  trivy sbom, exit %d", Pipeline.VULNERABLE);
        line("    3 unit tests        gradlew test, exit %d", Pipeline.TESTS_FAILED);
        line("    4 signature check   the %d checks of the verifier", Verifier.CHECKS.size());
        line("  THE CLEAN CHANGE:");
        line("    %d of %d gates passed, deployable: %s", release.gatesPassed(), Pipeline.GATES, yn(release.deployable()));
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("failing test", "the fee is amount / 10, not / 100", "unit tests", Pipeline.run(BankLib.origin(Flavour.FAILING_TEST), config(trusted, trusted))));
        cases.add(new Case("vulnerable library", "commons-text 1.9 in build.gradle", "dependency check",
                Pipeline.run(BankLib.origin(Flavour.VULNERABLE_DEPENDENCY), config(trusted, trusted))));
        cases.add(new Case("committed secret", "an access key in a properties file", "secret scan", Pipeline.run(BankLib.origin(Flavour.SECRET), config(trusted, trusted))));
        Pipeline.Config unsigned = config(trusted, trusted);
        unsigned.sign = false;
        cases.add(new Case("unsigned jar", "the signing step is skipped", "signature check", Pipeline.run(clean, unsigned)));
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
        cases.add(new Case("jar altered", "one bit changed after signing", "signature check", Pipeline.run(clean, altered)));
        cases.add(new Case("unknown key", "signed with the attacker's key", "signature check", Pipeline.run(clean, config(attacker, trusted))));
        line("  THE PLANTED FLAWS:");
        for (Case c : cases) {
            line("    %-19s %s", c.name(), c.planted());
        }
        line("  WHERE EACH ONE STOPPED (gates passed before it):");
        int stopped = 0;
        int rightGate = 0;
        for (Case c : cases) {
            boolean stop = !c.result().deployable();
            stopped += stop ? 1 : 0;
            rightGate += c.gate().equals(c.result().stoppedAt()) ? 1 : 0;
            line("    %-19s %-17s %d", c.name(), stop ? c.result().stoppedAt() : "LET THROUGH", c.result().gatesPassed());
        }
        line("    stopped %d of %d, each at the gate that owns it: %s", stopped, cases.size(), yn(rightGate == cases.size()));
        line("  WHAT EACH GATE SAW:");
        Pipeline.Result failing = cases.get(0).result();
        long tests = BankLib.LEDGER_TEST.lines().filter(l -> l.strip().equals("@Test")).count();
        line("    unit tests: %d of %d failed", failing.found().size(), tests);
        for (String f : failing.found()) {
            line("      %s", f.replace("()", "").replace("org.opentest4j.AssertionFailedError: ", "").replace("expected: <", "expected ")
                    .replace("> but was: <", ", got ").replace(">", ""));
        }
        line("      the fee of 10000: 10000 / 10 = %d, not 10000 / 100 = %d", 10_000 / 10, 10_000 / 100);
        String cve = cases.get(1).result().found().stream().filter(v -> v.startsWith("CVE-2022-42889 ")).findFirst().orElse("CVE-2022-42889 - not-reported");
        String[] v = cve.split(" ");
        line("    dependency check: %s, %s, %s %s", v[0], v[1], v[2].substring(v[2].indexOf(':') + 1), v[3]);
        for (String f : cases.get(2).result().found()) {
            line("    secret scan: %s", f);
        }
        for (Case c : cases.subList(3, cases.size())) {
            Pipeline.Result r = c.result();
            line("    signature check, %s: %s", c.name(), r.found().getFirst());
        }
    }

    // ------------------------------------------------------------------ FIVE

    /** A run with a broken tool, and what a pipeline that fails closed and one that fails open do with it. */
    private record Broken(String what, String change, String gate, Pipeline.Result closed, Pipeline.Result open) {
    }

    private void failClosed(Cosign.Keys trusted) throws Exception {
        line("FIVE. A gate whose tool cannot answer.");
        Git secret = BankLib.origin(Flavour.SECRET);
        Git vulnerable = BankLib.origin(Flavour.VULNERABLE_DEPENDENCY);
        Path badConfig = Scratch.file("gitleaks", ".toml");
        Files.writeString(badConfig, "this is [not = valid toml\n");
        Path emptyCache = Scratch.dir("trivycache");
        List<Broken> runs = new ArrayList<>();
        runs.add(broken("gitleaks missing", "committed secret", "secret scan", secret, trusted, c -> c.gitleaks = "gitleaks-not-installed"));
        runs.add(broken("gitleaks crashes", "committed secret", "secret scan", secret, trusted, c -> c.gitleaksExtra = List.of("--config", badConfig.toString())));
        runs.add(broken("trivy has no data", "vulnerable library", "dependency check", vulnerable, trusted, c -> c.trivyCache = emptyCache));
        line("  THE INPUT: THREE BROKEN TOOLS, EACH ON A FLAWED CHANGE:");
        line("    gitleaks missing   no gitleaks program, committed secret");
        line("    gitleaks crashes   its configuration is not TOML, same change");
        line("    trivy has no data  an empty database, vulnerable library");
        line("  WHAT EACH TOOL GAVE BACK (found would be the exit above):");
        for (Broken r : runs) {
            Gate.Answer a = r.closed().answer();
            line("    %-18s %-11s outcome %s", r.what(), a.exitText(), a.outcome());
        }
        line("  THE RESULT, THE SAME ERROR IN TWO PIPELINES:");
        line("    broken tool        fails closed                 fails open");
        int closed = 0;
        int open = 0;
        for (Broken r : runs) {
            boolean stopped = !r.closed().deployable() && r.gate().equals(r.closed().stoppedAt()) && r.closed().outcome() == Outcome.ERROR;
            closed += stopped ? 1 : 0;
            open += r.open().deployable() ? 1 : 0;
            line("    %-18s %-28s %s", r.what(), stopped ? "stopped at " + r.closed().stoppedAt() : "went on", r.open().deployable() ? "released" : "stopped");
        }
        line("    fails closed: %d of %d stopped; fails open: %d of %d released", closed, runs.size(), open, runs.size());
    }

    private Broken broken(String what, String change, String gate, Git origin, Cosign.Keys trusted, Consumer<Pipeline.Config> breakIt) throws Exception {
        Pipeline.Config closed = config(trusted, trusted);
        breakIt.accept(closed);
        Pipeline.Config open = config(trusted, trusted);
        breakIt.accept(open);
        open.failClosed = false;
        return new Broken(what, change, gate, Pipeline.run(origin, closed), Pipeline.run(origin, open));
    }

    // ------------------------------------------------------------------ SIX

    private void deploy(Git origin, Pipeline.Result release, Cosign.Keys trusted, Cosign.Keys attacker) throws Exception {
        line("SIX. Deploy only what verifies.");
        Verifier.Policy policy = new Verifier.Policy(Provenance.BUILDER, release.commit(), release.tree());
        Deploy deploy = new Deploy(Scratch.dir("production"), trusted.publicKey(), policy, new Cosign());
        line("  THE POLICY (what deploy trusts before it looks):");
        line("    the public key cosign.pub of the pipeline");
        line("    builder %s", policy.builderId());
        line("    commit %s, tree %s", shortId(policy.commit()), shortId(policy.tree()));
        line("  THE %d CHECKS, IN ORDER (the first that fails refuses):", Verifier.CHECKS.size());
        int half = (Verifier.CHECKS.size() + 1) / 2;
        for (int i = 0; i < half; i++) {
            String right = i + half < Verifier.CHECKS.size() ? (i + half + 1) + " " + Verifier.CHECKS.get(i + half) : "";
            line(String.format("    %-26s %s", (i + 1) + " " + Verifier.CHECKS.get(i), right).stripTrailing());
        }

        Bundle swapped = copy(release.bundle());
        byte[] bytes = Files.readAllBytes(swapped.jar());
        bytes[bytes.length / 2] ^= 1;
        Files.write(swapped.jar(), bytes);

        Bundle own = new Bundle(Scratch.dir("forged"));
        Files.write(own.jar(), (Files.readString(release.bundle().jar(), StandardCharsets.ISO_8859_1) + "backdoor").getBytes(StandardCharsets.ISO_8859_1));
        Files.writeString(own.provenance(), Provenance.create(new Provenance.Facts(Bundle.JAR, Digest.sha256(own.jar()), release.commit(), release.tree(),
                List.of(), Digest.sha256(new byte[0]))));
        Cosign.sign(attacker, own.jar(), own.jarSignature());
        Cosign.sign(attacker, own.provenance(), own.provenanceSignature());

        Pipeline.Result older = previous(origin, trusted);
        line("  THE RELEASES OFFERED:");
        line("    genuine   the release the pipeline made");
        line("    forged 1  the jar swapped, the genuine signatures kept");
        line("    forged 2  an own build and provenance, its own key");
        line("    forged 3  the genuine previous release, commit %s", shortId(older.commit()));
        line("  THE ANSWERS (checks passed, then the answer):");
        answer("genuine", deploy.deploy(release.bundle()));
        answer("forged 1", deploy.deploy(swapped));
        answer("forged 2", deploy.deploy(own));
        answer("forged 3", deploy.deploy(older.bundle()));
        line("    jars in production: %d", deploy.deployed());
    }

    private void answer(String name, Verifier.Verdict v) {
        line("    %-9s %d of %d  %s", name, v.checksPassed(), Verifier.CHECKS.size(), v.ok() ? "accepted" : "refused at " + v.failed());
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
