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

    private static void act1Sbom(StringBuilder out, Builder.Build release) throws Json.BadJsonException {
        line(out, "ONE. What a bill of materials answers, and what it does not.");
        List<Component> libraries = libraries();
        String sbom = Sbom.write("shop", "1.4.0", release.artifact(), libraries);
        line(out, "  the release " + ARTIFACT + " is built from " + main().files().size() + " files and depends on " + libraries.size() + " libraries");
        line(out, "  the CycloneDX 1.6 SBOM lists, each with a SHA-256: " + String.join(", ", Sbom.purls(sbom)));
        line(out, "  this SBOM is the one for this jar: " + Sbom.describes(sbom, release.artifact()));
        List<Component> tampered = List.of(libraries.get(0),
                new Component("com.acme", "acme-json", "2.4.1", "jar of acme-json@2.4.1, changed".getBytes(StandardCharsets.UTF_8)),
                libraries.get(2));
        line(out, "  a library jar changed after the SBOM was written is found: " + Sbom.changedComponents(sbom, tampered));
        line(out, "  which repository, branch and commit the jar was built from: the SBOM does not say");
        line(out, "  who built it, and on which machine: the SBOM does not say  <- THAT IS WHAT PROVENANCE IS FOR");
    }

    private static void act2Provenance(StringBuilder out, Builder.Build release) throws Exception {
        line(out, "TWO. Provenance: who built it, from what.");
        Dsse.Opened opened = Dsse.open(release.provenance(), POLICY.trustedKeys());
        Object statement = Json.read(new String(opened.payload(), StandardCharsets.UTF_8));
        line(out, "  an in-toto statement, predicate " + Json.text(statement, "predicateType"));
        line(out, "  subject: " + ARTIFACT + ", sha256 " + Digests.shortHex(Digests.sha256(release.artifact())) + "...");
        line(out, "  build type: " + Json.text(statement, "predicate", "buildDefinition", "buildType"));
        line(out, "  source: " + Json.text(statement, "predicate", "buildDefinition", "externalParameters", "repository")
                + ", " + Json.text(statement, "predicate", "buildDefinition", "externalParameters", "ref")
                + ", commit " + COMMIT.substring(0, 12) + "...");
        line(out, "  builder: " + Json.text(statement, "predicate", "runDetails", "builder", "id"));
        byte[] pae = Dsse.pae(Dsse.PAYLOAD_TYPE, opened.payload());
        String head = new String(pae, StandardCharsets.UTF_8);
        line(out, "  signed in a DSSE envelope over the encoding: " + head.substring(0, head.indexOf('{')) + "{...}");
        line(out, "  the Ed25519 signature verifies with the builder's key " + opened.keyId() + ": true");
        String retyped = release.provenance().replace(Dsse.PAYLOAD_TYPE, "application/json");
        line(out, "  the same signed bytes offered as another payload type: " + verdictOf(retyped));
    }

    private static String verdictOf(String envelope) {
        try {
            Dsse.open(envelope, POLICY.trustedKeys());
            return "accepted";
        } catch (Dsse.BadEnvelopeException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }

    private static void act3Gate(StringBuilder out, Builder.Build release) {
        line(out, "THREE. Checking before deploying.");
        line(out, "  the release from the trusted builder, built from main: " + POLICY.check(release.provenance(), ARTIFACT, release.artifact()));
        Source fork = new Source("https://git.example.org/mallory/store", MAIN, COMMIT, files("1.4.0-evil"));
        Builder.Build forkBuild = CI.build(fork, ARTIFACT, "build-1043");
        line(out, "  a build of a fork: " + POLICY.check(forkBuild.provenance(), ARTIFACT, forkBuild.artifact()));
        Source branch = new Source(REPOSITORY, "refs/heads/try-fast-checkout", COMMIT, files("1.4.0-try"));
        Builder.Build branchBuild = CI.build(branch, ARTIFACT, "build-1044");
        line(out, "  a build of an unreviewed branch: " + POLICY.check(branchBuild.provenance(), ARTIFACT, branchBuild.artifact()));
        Builder laptop = Builder.demo("https://laptop.dev.shop.example/ben", "ben-laptop", 9);
        Builder.Build laptopBuild = laptop.build(main(), ARTIFACT, "local-1");
        line(out, "  a build signed on a developer's laptop: " + POLICY.check(laptopBuild.provenance(), ARTIFACT, laptopBuild.artifact()));
        Builder staging = new Builder("https://build.shop.example/staging/v1", CI.keyId(), CI.key());
        Builder.Build stagingBuild = staging.build(main(), ARTIFACT, "stage-7");
        line(out, "  the trusted key, but a statement naming the staging runner: " + POLICY.check(stagingBuild.provenance(), ARTIFACT, stagingBuild.artifact()));
        line(out, "  the fork's statement edited after signing to say main's repository: "
                + POLICY.check(editPayload(forkBuild.provenance(), "https://git.example.org/mallory/store", REPOSITORY), ARTIFACT, forkBuild.artifact()));
        line(out, "  the fork's jar offered with the real release's provenance: " + POLICY.check(release.provenance(), ARTIFACT, forkBuild.artifact()));
        line(out, "  a jar with no provenance at all: " + POLICY.check(null, ARTIFACT, release.artifact()));
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
                new Pipeline("a developer's laptop build", false, false, false),
                new Pipeline("a laptop build that signs its own provenance", true, false, false),
                new Pipeline("the store's hosted CI runner", true, true, false),
                new Pipeline("a hardened build platform with isolated, short-lived builds", true, true, true));
        for (Pipeline p : pipelines) {
            line(out, "  " + p.name() + ": level " + p.level() + (p.missing().isEmpty() ? "" : ", held back because " + p.missing().get(0)));
        }
        Map<String, byte[]> files = main().files();
        List<String> listingA = List.of("com/shop/Store.class", "META-INF/MANIFEST.MF", "com/shop/Checkout.class");
        List<String> listingB = List.of("com/shop/Checkout.class", "com/shop/Store.class", "META-INF/MANIFEST.MF");
        byte[] carelessA = Archive.careless(listingA, files, LocalDateTime.of(2026, 10, 1, 9, 3, 12));
        byte[] carelessB = Archive.careless(listingB, files, LocalDateTime.of(2026, 10, 2, 14, 40, 51));
        line(out, "  two careless builds of the same commit (own clock, file-system order) give the same jar: "
                + Digests.same(Digests.sha256(carelessA), Digests.sha256(carelessB)));
        byte[] again = Archive.reproducible(files);
        line(out, "  two reproducible builds (commit time, sorted entries) give the same jar: "
                + Digests.same(Digests.sha256(release.artifact()), Digests.sha256(again)) + ", sha256 " + Digests.shortHex(Digests.sha256(again)) + "...");
        line(out, "  so anyone can rebuild the commit and check it against the provenance subject: "
                + POLICY.check(release.provenance(), ARTIFACT, again).allowed());
    }

    private static void act5Bill(StringBuilder out, Builder.Build release) throws Json.BadJsonException {
        line(out, "FIVE. The bill.");
        Source fork = new Source("https://git.example.org/mallory/store", MAIN, COMMIT, files("1.4.0-evil"));
        Builder.Build forkBuild = CI.build(fork, ARTIFACT, "build-1043");
        line(out, "  a deploy script that never checks runs the fork's build: deployed  <- PROVENANCE ONLY HELPS WHEN IT IS CHECKED");
        byte[] forged = Provenance.statement(ARTIFACT, forkBuild.artifact(), main(), BUILDER_ID, "build-1099");
        String forgedEnvelope = Dsse.envelope(forged, CI.keyId(), CI.key().getPrivate());
        line(out, "  with the builder's key stolen, a fork's jar with a statement claiming main: "
                + POLICY.check(forgedEnvelope, ARTIFACT, forkBuild.artifact()) + "  <- PROTECT THE KEY AND THE BUILDER");
        String oldSbom = Sbom.write("shop", "1.4.0", release.artifact(), libraries());
        byte[] next = Archive.reproducible(files("1.4.1"));
        line(out, "  the SBOM written for 1.4.0, checked against the 1.4.1 jar: describes it " + Sbom.describes(oldSbom, next)
                + "  <- WRITE THE SBOM AT EVERY BUILD");
        line(out, "  and a correct SBOM is only useful if something scans it against new advisories: see the -with-syft-and-grype version");
    }
}
