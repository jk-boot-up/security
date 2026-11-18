package com.jk.explore.security.provenance;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs the online store's release story in five numbered acts and prints exact, repeatable results. */
public class ProvenanceDemo {

    static final String REPOSITORY = "https://git.shop.example/shop/store";
    static final String MAIN = "refs/heads/main";
    static final String COMMIT = "4f1c2a9e8b7d6c5f4e3d2c1b0a9f8e7d6c5b4a39";
    static final String ARTIFACT = "shop-1.4.0.jar";
    static final String BUILDER_ID = "https://build.shop.example/runner/v1";

    static final Builder CI = Builder.demo(BUILDER_ID, "shop-ci-2026", 7);
    static final Policy POLICY = new Policy(Map.of(CI.keyId(), CI.key().getPublic()), BUILDER_ID, REPOSITORY, MAIN);

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static List<Component> libraries() {
        return List.of(
                library("com.acme", "acme-http", "3.0.2"),
                library("com.acme", "acme-json", "2.4.1"),
                library("com.acme", "acme-log", "1.9.0"));
    }

    private static Component library(String group, String name, String version) {
        return new Component(group, name, version, ("jar of " + name + "@" + version).getBytes(StandardCharsets.UTF_8));
    }

    static Map<String, byte[]> files(String storeVersion) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("META-INF/MANIFEST.MF", ("Manifest-Version: 1.0\nImplementation-Version: " + storeVersion + "\n").getBytes(StandardCharsets.UTF_8));
        files.put("com/shop/Store.class", ("store code " + storeVersion).getBytes(StandardCharsets.UTF_8));
        files.put("com/shop/Checkout.class", ("checkout code " + storeVersion).getBytes(StandardCharsets.UTF_8));
        return files;
    }

    static Source main() {
        return new Source(REPOSITORY, MAIN, COMMIT, files("1.4.0"));
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Builder.Build release = CI.build(main(), ARTIFACT, "build-1042");
        act1Sbom(out, release);
        act2Provenance(out, release);
        act3Gate(out, release);
        act4Levels(out, release);
        act5Bill(out, release);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A labelled row: two spaces of section indent, the label padded to a column, then the value. */
    private static void row(StringBuilder out, int width, String label, String value) {
        line(out, "    " + String.format("%-" + Math.max(width, label.length() + 2) + "s", label) + value);
    }

    private static String sha(byte[] bytes) {
        return "sha256 " + Digests.shortHex(Digests.sha256(bytes)) + "...";
    }

    private static Object statementOf(String envelope) throws Exception {
        return Json.read(new String(Dsse.open(envelope, POLICY.trustedKeys()).payload(), StandardCharsets.UTF_8));
    }

    private static void act1Sbom(StringBuilder out, Builder.Build release) throws Json.BadJsonException {
        line(out, "ONE. What a bill of materials answers, and what it does not.");
        List<Component> libraries = libraries();
        line(out, "  THE RELEASE (what the build used):");
        row(out, 11, "jar", ARTIFACT + ", " + main().files().size() + " files, " + libraries.size() + " libraries");
        String label = "files";
        for (String file : main().files().keySet()) {
            row(out, 11, label, file);
            label = "";
        }
        label = "libraries";
        for (Component c : libraries) {
            row(out, 11, label, c.group() + " " + c.name() + " " + c.version());
            label = "";
        }
        String sbom = Sbom.write("shop", "1.4.0", release.artifact(), libraries);
        line(out, "  THE SBOM (CycloneDX 1.6, what it records):");
        row(out, 13, "application", "shop 1.4.0, " + sha(release.artifact()));
        for (Component c : libraries) {
            row(out, 13, "component", c.purl());
            row(out, 13, "", "sha256 " + c.sha256().substring(0, 16) + "...");
        }
        Component json = libraries.get(1);
        line(out, "  HOW ONE ENTRY IS MADE (" + json.name() + "):");
        row(out, 9, "purl", "pkg:maven / " + json.group() + " / " + json.name() + " @ " + json.version());
        row(out, 9, "bytes", "\"" + new String(json.jar(), StandardCharsets.UTF_8) + "\" (" + json.jar().length + " bytes)");
        row(out, 9, "sha256", json.sha256().substring(0, 32) + "...");
        Component changed = new Component(json.group(), json.name(), json.version(),
                "jar of acme-json@2.4.1, changed".getBytes(StandardCharsets.UTF_8));
        List<Component> onDisk = List.of(libraries.get(0), changed, libraries.get(2));
        line(out, "  CHECKING THE SBOM:");
        row(out, 25, "application hash = jar?", Sbom.describes(sbom, release.artifact()) ? "yes, the SBOM is for this jar" : "no");
        row(out, 25, json.name() + " in the SBOM", "sha256 " + json.sha256().substring(0, 16) + "...");
        row(out, 25, json.name() + " on disk now", "sha256 " + changed.sha256().substring(0, 16) + "...");
        row(out, 25, "changed after the SBOM", String.join(", ", Sbom.changedComponents(sbom, onDisk)).replace("pkg:maven/com.acme/", ""));
        line(out, "  WHAT THE SBOM DOES NOT SAY:");
        row(out, 28, "repository, branch, commit", "not in it");
        row(out, 28, "who built it, on what", "not in it  <- PROVENANCE");
    }

    private static void act2Provenance(StringBuilder out, Builder.Build release) throws Exception {
        line(out, "TWO. Provenance: who built it, from what.");
        Object statement = statementOf(release.provenance());
        line(out, "  THE STATEMENT (in-toto, written by the builder):");
        row(out, 15, "_type", Json.text(statement, "_type"));
        row(out, 15, "subject", ARTIFACT);
        row(out, 15, "", sha(release.artifact()));
        row(out, 15, "predicateType", Json.text(statement, "predicateType"));
        row(out, 15, "buildType", Json.text(statement, "predicate", "buildDefinition", "buildType"));
        row(out, 15, "repository", Json.text(statement, "predicate", "buildDefinition", "externalParameters", "repository"));
        row(out, 15, "ref", Json.text(statement, "predicate", "buildDefinition", "externalParameters", "ref"));
        Object resolved = ((List<?>) Json.at(statement, "predicate", "buildDefinition", "resolvedDependencies")).get(0);
        row(out, 15, "gitCommit", Json.text(resolved, "digest", "gitCommit").substring(0, 12) + "...");
        row(out, 15, "builder.id", Json.text(statement, "predicate", "runDetails", "builder", "id"));
        row(out, 15, "invocationId", Json.text(statement, "predicate", "runDetails", "metadata", "invocationId"));
        Object envelope = Json.read(release.provenance());
        byte[] payload = java.util.Base64.getDecoder().decode(Json.text(envelope, "payload"));
        byte[] typeBytes = Dsse.PAYLOAD_TYPE.getBytes(StandardCharsets.UTF_8);
        String pae = new String(Dsse.pae(Dsse.PAYLOAD_TYPE, payload), StandardCharsets.UTF_8);
        line(out, "  HOW THE SIGNED BYTES ARE MADE (DSSE PAE):");
        row(out, 8, "rule", "DSSEv1 len(type) type len(body) body");
        row(out, 8, "type", Dsse.PAYLOAD_TYPE + " = " + typeBytes.length + " bytes");
        row(out, 8, "body", "the statement = " + payload.length + " bytes");
        row(out, 8, "signed", pae.substring(0, pae.indexOf('{')) + "{...");
        Object signature = ((List<?>) Json.at(envelope, "signatures")).get(0);
        byte[] sig = java.util.Base64.getDecoder().decode(Json.text(signature, "sig"));
        line(out, "  THE ENVELOPE (published next to the jar):");
        row(out, 13, "payloadType", Json.text(envelope, "payloadType"));
        row(out, 13, "payload", "base64 of the " + payload.length + " bytes, " + Json.text(envelope, "payload").length() + " chars");
        row(out, 13, "keyid", Json.text(signature, "keyid"));
        row(out, 13, "sig", "Ed25519, " + sig.length + " bytes: " + Digests.hex(sig).substring(0, 16) + "...");
        java.security.PublicKey key = POLICY.trustedKeys().get(CI.keyId());
        line(out, "  VERIFYING THE ENVELOPE:");
        row(out, 13, "Ed25519", "key " + CI.keyId() + ", over the signed bytes: " + Ed25519.verify(key, Dsse.pae(Dsse.PAYLOAD_TYPE, payload), sig));
        String other = "application/json";
        String otherPae = new String(Dsse.pae(other, payload), StandardCharsets.UTF_8);
        line(out, "  THE SAME BYTES, OFFERED AS ANOTHER TYPE:");
        row(out, 14, "payloadType", other);
        row(out, 14, "signed bytes", otherPae.substring(0, otherPae.indexOf('{')) + "{...");
        row(out, 14, "Ed25519", "over those bytes: " + Ed25519.verify(key, Dsse.pae(other, payload), sig));
        row(out, 14, "the gate", verdictOf(release.provenance().replace(Dsse.PAYLOAD_TYPE, other)));
    }

    private static String verdictOf(String envelope) {
        try {
            Dsse.open(envelope, POLICY.trustedKeys());
            return "accepted";
        } catch (Dsse.BadEnvelopeException e) {
            return "refused: " + e.getMessage();
        }
    }

    static final String FORK = "https://git.example.org/mallory/store";

    private static void act3Gate(StringBuilder out, Builder.Build release) throws Exception {
        line(out, "THREE. Checking before deploying.");
        line(out, "  THE GATE'S POLICY (what it trusts):");
        row(out, 12, "key", CI.keyId() + " (an Ed25519 public key)");
        row(out, 12, "builder", POLICY.builderId());
        row(out, 12, "repository", POLICY.repository());
        row(out, 12, "ref", POLICY.ref());
        line(out, "  THE CHECKS, IN ORDER:");
        StringBuilder checks = new StringBuilder();
        for (int i = 0; i < Policy.CHECKS.size(); i++) {
            checks.append(i == 4 ? "\n    " : i == 0 ? "" : "  ").append(i + 1).append(' ').append(Policy.CHECKS.get(i));
        }
        line(out, "    " + checks);
        Source fork = new Source(FORK, MAIN, COMMIT, files("1.4.0-evil"));
        Builder.Build forkBuild = CI.build(fork, ARTIFACT, "build-1043");
        Source branch = new Source(REPOSITORY, "refs/heads/try-fast-checkout", COMMIT, files("1.4.0-try"));
        Builder.Build branchBuild = CI.build(branch, ARTIFACT, "build-1044");
        Builder laptop = Builder.demo("https://laptop.dev.shop.example/ben", "ben-laptop", 9);
        Builder.Build laptopBuild = laptop.build(main(), ARTIFACT, "local-1");
        Builder staging = new Builder("https://build.shop.example/staging/v1", CI.keyId(), CI.key());
        Builder.Build stagingBuild = staging.build(main(), ARTIFACT, "stage-7");
        String edited = editPayload(forkBuild.provenance(), FORK, REPOSITORY);
        String[][] offers = {
                {"the release from the trusted builder, from main", release.provenance(), "release"},
                {"a build of a fork", forkBuild.provenance(), "fork"},
                {"a build of an unreviewed branch", branchBuild.provenance(), "branch"},
                {"a build signed on a developer's laptop", laptopBuild.provenance(), "laptop"},
                {"the trusted key, naming the staging runner", stagingBuild.provenance(), "staging"},
                {"the fork's statement, edited to say main's repo", edited, "fork"},
                {"the fork's jar with the release's provenance", release.provenance(), "fork"},
                {"a jar with no provenance at all", null, "release"},
        };
        Map<String, byte[]> jars = Map.of("release", release.artifact(), "fork", forkBuild.artifact(), "branch", branchBuild.artifact(),
                "laptop", laptopBuild.artifact(), "staging", stagingBuild.artifact());
        line(out, "  EIGHT RELEASES OFFERED (the input):");
        for (int i = 0; i < offers.length; i++) {
            line(out, "    " + (char) ('A' + i) + "  " + offers[i][0]);
        }
        line(out, "  THE GATE'S ANSWERS (the result):");
        for (int i = 0; i < offers.length; i++) {
            Verdict v = POLICY.check(offers[i][1], ARTIFACT, jars.get(offers[i][2]));
            String answer = v.allowed() ? "deployed      all " + Policy.CHECKS.size() + " checks pass" : "refused at " + v.failedCheck() + "  " + v.reason();
            line(out, "    " + (char) ('A' + i) + "  " + answer);
        }
        line(out, "  HOW TWO REFUSALS ARE MADE:");
        row(out, 3, "F", "the signed statement " + payloadOf(forkBuild.provenance()).length + " bytes, names the fork");
        row(out, 3, "", "the edited statement " + payloadOf(edited).length + " bytes, names main's repo");
        row(out, 3, "", "Ed25519 over the edited bytes: "
                + Ed25519.verify(CI.key().getPublic(), Dsse.pae(Dsse.PAYLOAD_TYPE, payloadOf(edited)), sigOf(forkBuild.provenance())));
        Object subject = ((List<?>) Json.at(statementOf(release.provenance()), "subject")).get(0);
        row(out, 3, "G", "the subject  sha256 " + Json.text(subject, "digest", "sha256").substring(0, 16) + "...");
        row(out, 3, "", "the jar      " + sha(forkBuild.artifact()));
    }

    private static byte[] payloadOf(String envelope) throws Json.BadJsonException {
        return java.util.Base64.getDecoder().decode(Json.text(Json.read(envelope), "payload"));
    }

    private static byte[] sigOf(String envelope) throws Json.BadJsonException {
        return java.util.Base64.getDecoder().decode(Json.text(((List<?>) Json.at(Json.read(envelope), "signatures")).get(0), "sig"));
    }

    /** Decodes an envelope's payload, replaces text in it, and puts it back without signing it again, as a tamperer would. */
    static String editPayload(String envelope, String from, String to) {
        try {
            Object root = Json.read(envelope);
            String payload = new String(java.util.Base64.getDecoder().decode(Json.text(root, "payload")), StandardCharsets.UTF_8);
            String edited = java.util.Base64.getEncoder().encodeToString(payload.replace(from, to).getBytes(StandardCharsets.UTF_8));
            return envelope.replace(Json.text(root, "payload"), edited);
        } catch (Json.BadJsonException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void act4Levels(StringBuilder out, Builder.Build release) {
        line(out, "FOUR. SLSA build levels, and builds that can be repeated.");
        List<Pipeline> pipelines = List.of(
                new Pipeline("laptop", false, false, false),
                new Pipeline("laptop, signed", true, false, false),
                new Pipeline("hosted CI", true, true, false),
                new Pipeline("hardened platform", true, true, true));
        line(out, "  FOUR PIPELINES (what each one does):");
        line(out, "    pipeline           provenance  signed by   key isolated");
        for (Pipeline p : pipelines) {
            line(out, "    " + String.format("%-19s%-12s%-12s%s", p.name(), p.writesProvenance() ? "yes" : "no",
                    !p.writesProvenance() ? "-" : p.hostedPlatformSigns() ? "platform" : "developer",
                    p.buildsIsolatedFromSigningKey() ? "yes" : "no"));
        }
        line(out, "  THEIR SLSA BUILD LEVELS:");
        for (Pipeline p : pipelines) {
            line(out, "    " + String.format("%-19slevel %d  %s", p.name(), p.level(), p.missing().isEmpty() ? "" : p.missing().get(0)).stripTrailing());
        }
        Map<String, byte[]> files = main().files();
        List<String> listingA = List.of("com/shop/Store.class", "META-INF/MANIFEST.MF", "com/shop/Checkout.class");
        List<String> listingB = List.of("com/shop/Checkout.class", "com/shop/Store.class", "META-INF/MANIFEST.MF");
        LocalDateTime clockA = LocalDateTime.of(2026, 10, 1, 9, 3, 12);
        LocalDateTime clockB = LocalDateTime.of(2026, 10, 2, 14, 40, 51);
        byte[] carelessA = Archive.careless(listingA, files, clockA);
        byte[] carelessB = Archive.careless(listingB, files, clockB);
        line(out, "  TWO CARELESS BUILDS OF ONE COMMIT (the input):");
        row(out, 10, "build 1", "clock " + clockA.toString().replace('T', ' '));
        row(out, 10, "", "order " + shortNames(listingA));
        row(out, 10, "build 2", "clock " + clockB.toString().replace('T', ' '));
        row(out, 10, "", "order " + shortNames(listingB));
        line(out, "  THEIR JARS (the result):");
        row(out, 10, "build 1", sha(carelessA));
        row(out, 10, "build 2", sha(carelessB));
        row(out, 10, "same jar", Digests.same(Digests.sha256(carelessA), Digests.sha256(carelessB)) ? "yes" : "no");
        byte[] again = Archive.reproducible(files);
        List<String> sorted = new java.util.ArrayList<>(new java.util.TreeMap<>(files).keySet());
        line(out, "  TWO REPRODUCIBLE BUILDS (the input):");
        row(out, 10, "both", "time " + Archive.COMMIT_TIME.toString().replace('T', ' ') + ":00, the commit time");
        row(out, 10, "", "order sorted by name: " + shortNames(sorted));
        line(out, "  THEIR JARS (the result):");
        row(out, 10, "build 1", sha(release.artifact()));
        row(out, 10, "build 2", sha(again));
        row(out, 10, "same jar", Digests.same(Digests.sha256(release.artifact()), Digests.sha256(again)) ? "yes" : "no");
        row(out, 10, "the gate", "the rebuild, with the provenance: " + POLICY.check(release.provenance(), ARTIFACT, again));
    }

    /** File names without their folders and the .class suffix, so a build order fits on one line. */
    private static String shortNames(List<String> names) {
        return String.join(", ", names.stream().map(n -> n.substring(n.lastIndexOf('/') + 1).replace(".class", "")).toList());
    }

    private static void act5Bill(StringBuilder out, Builder.Build release) throws Json.BadJsonException {
        line(out, "FIVE. The bill.");
        Source fork = new Source(FORK, MAIN, COMMIT, files("1.4.0-evil"));
        Builder.Build forkBuild = CI.build(fork, ARTIFACT, "build-1043");
        line(out, "  NO GATE AT DEPLOY (a script that never checks):");
        row(out, 13, "the fork's build", "deployed  <- CHECK PROVENANCE BEFORE IT RUNS");
        byte[] forged = Provenance.statement(ARTIFACT, forkBuild.artifact(), main(), BUILDER_ID, "build-1099");
        String forgedEnvelope = Dsse.envelope(forged, CI.keyId(), CI.key().getPrivate());
        Object forgedStatement = Json.read(new String(forged, StandardCharsets.UTF_8));
        line(out, "  A STOLEN BUILDER KEY (a forged statement):");
        row(out, 13, "subject", "the fork's jar, " + sha(forkBuild.artifact()));
        row(out, 13, "repository", Json.text(forgedStatement, "predicate", "buildDefinition", "externalParameters", "repository") + " (a lie)");
        row(out, 13, "signed with", CI.keyId());
        row(out, 13, "the gate", POLICY.check(forgedEnvelope, ARTIFACT, forkBuild.artifact()) + "  <- PROTECT THE KEY AND THE BUILDER");
        String oldSbom = Sbom.write("shop", "1.4.0", release.artifact(), libraries());
        byte[] next = Archive.reproducible(files("1.4.1"));
        line(out, "  A STALE SBOM (written for 1.4.0):");
        row(out, 14, "SBOM records", "shop 1.4.0, " + sha(release.artifact()));
        row(out, 14, "1.4.1 jar", sha(next));
        row(out, 14, "describes it", (Sbom.describes(oldSbom, next) ? "yes" : "no") + "  <- WRITE THE SBOM AT EVERY BUILD");
        line(out, "  AND A CORRECT SBOM STILL NEEDS SCANNING:");
        line(out, "    against new advisories: the -with-syft-and-grype version");
    }
}
