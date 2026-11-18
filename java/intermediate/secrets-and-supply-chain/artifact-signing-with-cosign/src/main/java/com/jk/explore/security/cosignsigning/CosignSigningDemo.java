package com.jk.explore.security.cosignsigning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the store's release-signing story in five numbered acts with the real cosign command and a real container registry on the
 * loopback interface. Each act prints labelled sections in capitals, the input first and then the result. Digests, keys and ports change
 * from machine to machine, so they are never printed: each digest is shown by a letter, in the order the demo first meets it.
 */
public class CosignSigningDemo {

    static final String APP = "shop/app";
    static final String TAG = "2.4.0";
    static final String CI = "https://ci.shop.example";

    /** The answer of the deployment gate, with the reason. */
    record Verdict(boolean accepted, String reason) {

        String word() {
            return accepted ? "accepted" : "refused";
        }

        @Override
        public String toString() {
            return word() + " (" + reason + ")";
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        Map<String, String> letters = new LinkedHashMap<>();
        try (Lab lab = Lab.start()) {
            Cosign cosign = new Cosign(lab.keys());
            for (String key : List.of("release", "attacker", "old", "next")) {
                cosign.generateKeyPair(key);
            }
            Demo demo = new Demo(out, lab, cosign, letters);
            demo.act1MutableTags();
            demo.act2SignedRelease();
            demo.act3ClaimsAndKeys();
            demo.act4Attestations();
            demo.act5Bill();
        }
        return out.toString();
    }

    /** The deployment gate: resolve the tag to a digest, then accept only a digest that one of the trusted keys signed with the required claims. */
    static Verdict gate(Lab lab, Cosign cosign, String tag, List<String> trustedKeys, Map<String, String> required) throws IOException, InterruptedException {
        return gate(lab, cosign, tag, trustedKeys, required, APP);
    }

    static Verdict gate(Lab lab, Cosign cosign, String tag, List<String> trustedKeys, Map<String, String> required, String repository)
            throws IOException, InterruptedException {
        String digest = lab.resolve(repository, tag);
        String last = "no signature for this digest";
        for (String key : trustedKeys) {
            Cosign.Result r = cosign.verify(key, digest, required);
            if (r.ok()) {
                return new Verdict(true, "signed by the " + key + " key");
            }
            last = r.why();
        }
        return new Verdict(false, last);
    }

    /** One run of the five acts, with the place the output goes and the letters given to digests. */
    private record Demo(StringBuilder out, Lab lab, Cosign cosign, Map<String, String> letters) {

        private void line(String text) {
            out.append(text).append('\n');
        }

        private void section(String text) {
            line("  " + text);
        }

        private void row(int width, String label, String value) {
            line("    " + (width == 0 ? "" : String.format("%-" + width + "s", label)) + value);
        }

        private void verdict(int width, String label, String word, String reason) {
            row(width, label, String.format("%-10s", word) + reason);
        }

        private void verdict(int width, String label, Verdict v) {
            verdict(width, label, v.word(), v.reason());
        }

        /** "digest A", "digest B", ... in the order the demo first meets each digest. */
        private String letter(String digestRef) {
            String digest = digestRef.substring(digestRef.indexOf("sha256:"));
            return "digest " + letters.computeIfAbsent(digest, d -> String.valueOf((char) ('A' + letters.size())));
        }

        void act1MutableTags() throws IOException, InterruptedException {
            line("ONE. A deployment that runs whatever a tag points at.");
            String release = lab.push(Lab.RELEASE_IMAGE, APP, TAG);
            section("BEFORE (the release team pushes " + APP + ":" + TAG + "):");
            row(12, "tag " + TAG, "-> " + letter(lab.resolve(APP, TAG)) + ", the release");
            row(12, "a digest", "sha256 of the image's manifest: a checksum");
            row(12, "pull " + TAG, "gets the release: " + lab.resolve(APP, TAG).equals(release));
            String attacker = lab.push(Lab.ATTACKER_IMAGE, APP, TAG);
            section("AFTER (someone with push rights reuses the tag):");
            row(12, "tag " + TAG, "-> " + letter(lab.resolve(APP, TAG)) + ", another image");
            row(12, "the same?", letter(release) + " = " + letter(attacker) + ": " + release.equals(attacker));
            row(12, "pull " + TAG, "gets the release: " + lab.resolve(APP, TAG).equals(release) + "  <- A TAG IS A NAME");
            row(12, "pull by " + letter(release).substring(7), "gets the release: " + !release.equals(lab.resolve(APP, TAG)));
            section("WHY A DIGEST ALONE IS NOT ENOUGH:");
            line("    it must come from somewhere that cannot be changed");
            lab.push(Lab.RELEASE_IMAGE, APP, TAG);
        }

        void act2SignedRelease() throws IOException, InterruptedException {
            line("TWO. A signed release, checked before it runs.");
            String release = lab.resolve(APP, TAG);
            boolean signed = cosign.sign("release", release, Map.of()).ok();
            SignedPayload payload = Cosign.payloadsOf(cosign.verify("release", release, Map.of()).output()).getFirst();
            section("BEFORE SIGNING (what cosign signs for " + letter(release) + "):");
            row(9, "type", payload.type());
            row(9, "digest", "docker-manifest-digest = " + letter(payload.digest()));
            String repository = payload.reference().substring(payload.reference().indexOf('/') + 1).split("@")[0];
            row(9, "for", "docker-reference = .../" + repository + "@<" + letter(release) + ">");
            row(9, "claims", payload.claimsText());
            section("AFTER SIGNING (with the release key):");
            row(11, "signed", String.valueOf(signed));
            String hex = release.substring(release.indexOf("sha256:") + 7);
            java.util.regex.Matcher tag = java.util.regex.Pattern.compile("\"(sha256-" + hex + "[^\"]*)\"").matcher(lab.tags(APP));
            row(11, "stored as", tag.find() ? "one more tag: " + tag.group(1).replace(hex, "<" + letter(release) + ">") : "no tag");
            row(11, "the tag", "not signed: the payload names the digest");
            section("THE GATE (pins release.pub, resolves the tag):");
            verdict(24, "tag -> " + letter(release), gate(lab, cosign, TAG, List.of("release"), Map.of()));
            String attackerDigest = lab.push(Lab.ATTACKER_IMAGE, APP, TAG);
            verdict(24, "moved -> " + letter(attackerDigest), gate(lab, cosign, TAG, List.of("release"), Map.of()));
            cosign.sign("attacker", attackerDigest, Map.of());
            verdict(24, letter(attackerDigest) + ", attacker key", gate(lab, cosign, TAG, List.of("release"), Map.of()));
            lab.push(Lab.RELEASE_IMAGE, APP, TAG);
            verdict(24, "back -> " + letter(release), gate(lab, cosign, TAG, List.of("release"), Map.of()));
        }

        void act3ClaimsAndKeys() throws IOException, InterruptedException {
            line("THREE. Signed claims, and keys that rotate.");
            String release = lab.resolve(APP, TAG);
            cosign.sign("release", release, Map.of("channel", "stable", "version", TAG));
            SignedPayload withClaims = Cosign.payloadsOf(cosign.verify("release", release, Map.of("channel", "stable")).output()).stream()
                    .filter(p -> !p.claims().isEmpty()).findFirst().orElseThrow();
            section("BEFORE (" + letter(release) + " signed again, with claims):");
            row(9, "claims", withClaims.claimsText());
            row(9, "signed", "inside the payload: they cannot be edited");
            String beta = lab.push(Lab.BETA_IMAGE, APP, "2.5.0-beta");
            cosign.sign("release", beta, Map.of("channel", "beta"));
            row(9, "a beta", letter(beta) + ", claims channel=beta");
            section("AFTER (gates that require a claim):");
            verdict(24, "stable gate, " + letter(release), gate(lab, cosign, TAG, List.of("release"), Map.of("channel", "stable")));
            verdict(24, "beta gate, " + letter(release), gate(lab, cosign, TAG, List.of("release"), Map.of("channel", "beta")));
            verdict(24, "stable gate, " + letter(beta), gate(lab, cosign, "2.5.0-beta", List.of("release"), Map.of("channel", "stable")));
            section("THE ROTATION (from the release key to next):");
            verdict(24, "trusts both, " + letter(release), gate(lab, cosign, TAG, List.of("release", "next"), Map.of()));
            cosign.sign("next", release, Map.of());
            verdict(24, "trusts next, " + letter(release), gate(lab, cosign, TAG, List.of("next"), Map.of()));
            verdict(24, "trusts next, " + letter(beta), gate(lab, cosign, "2.5.0-beta", List.of("next"), Map.of()));
            String late = lab.push(Lab.THIEF_IMAGE, APP, "9.9.9");
            cosign.sign("old", late, Map.of());
            section("BEFORE: A THIEF WITH THE RETIRED old KEY, TODAY:");
            row(9, "image", letter(late) + ", signed with old");
            row(9, "time", "none: a signature carries no date");
            section("AFTER: THE GATES:");
            verdict(24, "still trusts old", gate(lab, cosign, "9.9.9", List.of("old"), Map.of()));
            line("    <- NO LOG, SO NO DAY TO REVOKE FROM");
            verdict(24, "dropped old", gate(lab, cosign, "9.9.9", List.of("release", "next"), Map.of()));
        }

        void act4Attestations() throws IOException, InterruptedException {
            line("FOUR. Signed statements about how the image was built.");
            String release = lab.resolve(APP, TAG);
            String source = "git+https://git.shop.example/shop@abc123";
            Path predicate = Files.writeString(lab.keys().resolve("provenance.json"),
                    "{\"builder\":{\"id\":\"" + CI + "\"},\"source\":\"" + source + "\",\"reproducible\":false}");
            boolean attested = cosign.attest("release", release, "slsaprovenance", predicate).ok();
            section("BEFORE (the provenance the CI system signs):");
            row(9, "image", letter(release));
            row(9, "builder", CI);
            row(9, "source", source);
            row(9, "signed", "with the release key, type slsaprovenance: " + attested);
            Path laptop = Files.writeString(lab.keys().resolve("laptop.json"), "{\"builder\":{\"id\":\"https://laptop.local\"},\"source\":\"a working copy\"}");
            String other = lab.push(Lab.LAPTOP_IMAGE, APP, "2.4.1");
            cosign.attest("release", other, "slsaprovenance", laptop);
            section("AFTER (the signature, then the gate's rule: builder = CI):");
            row(15, "image", String.format("%-11s%-24s%s", "signature", "builder", "rule"));
            for (String digest : List.of(release, other)) {
                Cosign.Result r = cosign.verifyAttestation("release", digest, "slsaprovenance");
                Object builder = ((Map<?, ?>) Cosign.predicateOf(r.output()).get("builder")).get("id");
                row(15, letter(digest), String.format("%-11s%-24s%s", r.word(), builder, CI.equals(builder) ? "accepted" : "refused"));
            }
            line("    <- A SIGNATURE SAYS WHO; THE RULE CHECKS WHAT");
            String forged = lab.push(Lab.THIEF_IMAGE, APP, "forged");
            cosign.attest("attacker", forged, "slsaprovenance", predicate);
            String unsigned = lab.push(Lab.UNSIGNED_IMAGE, APP, "unsigned");
            section("FORGED AND BORROWED ATTESTATIONS:");
            Cosign.Result byAttacker = cosign.verifyAttestation("release", forged, "slsaprovenance");
            verdict(23, "signed by attacker", byAttacker.word(), byAttacker.why());
            Cosign.Result borrowed = cosign.verifyAttestation("release", unsigned, "slsaprovenance");
            verdict(23, "for " + letter(unsigned) + " instead", borrowed.word(), borrowed.why());
            section("LEFT OUT IN THIS LOCAL LAB:");
            line("    the public transparency log, where a monitor reads");
            line("    every signature");
        }

        void act5Bill() throws IOException, InterruptedException {
            line("FIVE. The bill.");
            String unsigned = lab.resolve(APP, "unsigned");
            section("THE GATE GUARDS ONE PATH:");
            verdict(18, "unsigned image", gate(lab, cosign, "unsigned", List.of("release"), Map.of()));
            row(18, "a direct pull", "gets " + letter(unsigned) + " anyway: " + lab.resolve(APP, "unsigned").equals(unsigned));
            section("THE SIGNING KEY IS THE CROWN JEWEL:");
            boolean wrong = cosign.sign("release", "wrong-passphrase", lab.resolve(APP, TAG), Map.of()).ok();
            row(18, "wrong passphrase", wrong ? "signed" : "cannot sign");
            section("ROTATION IS A PROJECT:");
            line("    every gate trusts the new key before the old one goes,");
            line("    and nothing marks the day an old signature was made");
            section("KEYLESS SIGNING:");
            line("    short-lived certificates tied to an identity, logged");
            line("    in public: needs the Fulcio and Rekor services");
            section("COSIGN 3:");
            line("    offline signing moves behind a signing config,");
            line("    so the flags used here may change");
        }
    }
}
