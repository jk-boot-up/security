package com.jk.explore.security.cosignsigning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs the store's release-signing story in five numbered acts with the real cosign command and a real container registry on the loopback interface. */
public class CosignSigningDemo {

    static final String APP = "shop/app";
    static final String TAG = "2.4.0";
    static final String CI = "https://ci.shop.example";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Digests, keys and ports are never printed. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Lab lab = Lab.start()) {
            Cosign cosign = new Cosign(lab.keys());
            for (String key : List.of("release", "attacker", "old", "next")) {
                cosign.generateKeyPair(key);
            }
            act1MutableTags(out, lab);
            act2SignedRelease(out, lab, cosign);
            act3ClaimsAndKeys(out, lab, cosign);
            act4Attestations(out, lab, cosign);
            act5Bill(out, lab, cosign);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The deployment gate: resolve the tag to a digest, then accept only a digest that one of the trusted keys signed with the required claims. */
    static String gate(Lab lab, Cosign cosign, String tag, List<String> trustedKeys, Map<String, String> required) throws IOException, InterruptedException {
        return gate(lab, cosign, tag, trustedKeys, required, APP);
    }

    static String gate(Lab lab, Cosign cosign, String tag, List<String> trustedKeys, Map<String, String> required, String repository) throws IOException, InterruptedException {
        String digest = lab.resolve(repository, tag);
        String last = "refused (no signature for this digest)";
        for (String key : trustedKeys) {
            Cosign.Result r = cosign.verify(key, digest, required);
            if (r.ok()) {
                return "accepted (signed by the " + key + " key)";
            }
            last = r.reason();
        }
        return last;
    }

    private static void act1MutableTags(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "ONE. A deployment that runs whatever a tag points at.");
        String release = lab.push(Lab.RELEASE_IMAGE, APP, TAG);
        line(out, "  the release team pushes the image as " + APP + ":" + TAG + " to the registry: the tag points at one digest, the checksum of that image's manifest");
        line(out, "  the deployment pulls by tag: it gets exactly that image: " + lab.resolve(APP, TAG).equals(release));
        String attacker = lab.push(Lab.ATTACKER_IMAGE, APP, TAG);
        line(out, "  someone with push rights, or a registry that is not what it was, pushes a different image under the same tag: the digests differ: " + !release.equals(attacker));
        line(out, "  the deployment pulls by tag again: it gets the other image: " + lab.resolve(APP, TAG).equals(attacker) + "  <- A TAG IS A NAME, NOT A CHECKSUM");
        line(out, "  the deployment pulls by the digest it was given: it gets the release: " + !release.equals(lab.resolve(APP, TAG)) + ", but only if the digest itself came from somewhere that cannot be changed");
        lab.push(Lab.RELEASE_IMAGE, APP, TAG);
    }

    private static void act2SignedRelease(StringBuilder out, Lab lab, Cosign cosign) throws IOException, InterruptedException {
        line(out, "TWO. A signed release, checked before it runs.");
        String release = lab.resolve(APP, TAG);
        line(out, "  the release pipeline signs the image digest with the release key: " + (cosign.sign("release", release, Map.of()).ok() ? "signed" : "failed"));
        boolean stored = lab.tags(APP).contains("sha256-" + release.substring(release.indexOf("sha256:") + 7) + ".sig") || lab.tags(APP).contains(".sig") || lab.tags(APP).contains("sha256-");
        line(out, "  the signature is stored in the same registry, next to the image, as one more tag: " + stored);
        line(out, "  the deployment pins the public key of the release key and checks before it deploys: " + gate(lab, cosign, TAG, List.of("release"), Map.of()));
        lab.push(Lab.ATTACKER_IMAGE, APP, TAG);
        line(out, "  the tag is moved to the attacker's image; the same gate: " + gate(lab, cosign, TAG, List.of("release"), Map.of()));
        String attackerDigest = lab.resolve(APP, TAG);
        cosign.sign("attacker", attackerDigest, Map.of());
        line(out, "  the attacker signs their own image with a key of their own: " + gate(lab, cosign, TAG, List.of("release"), Map.of()));
        line(out, "  a key the deployment does not know is not trusted, however well the signature was made");
        lab.push(Lab.RELEASE_IMAGE, APP, TAG);
        line(out, "  the tag is put back; the gate: " + gate(lab, cosign, TAG, List.of("release"), Map.of()));
    }

    private static void act3ClaimsAndKeys(StringBuilder out, Lab lab, Cosign cosign) throws IOException, InterruptedException {
        line(out, "THREE. Signed claims, and keys that rotate.");
        String release = lab.resolve(APP, TAG);
        cosign.sign("release", release, Map.of("channel", "stable", "version", TAG));
        line(out, "  the release signs the digest again with the claims channel=stable and version=" + TAG);
        line(out, "  a gate that requires channel=stable: " + gate(lab, cosign, TAG, List.of("release"), Map.of("channel", "stable")));
        line(out, "  a gate that requires channel=beta: " + gate(lab, cosign, TAG, List.of("release"), Map.of("channel", "beta")) + "  (the claims are signed with the image, so they cannot be edited)");
        String beta = lab.push(Lab.BETA_IMAGE, APP, "2.5.0-beta");
        cosign.sign("release", beta, Map.of("channel", "beta"));
        line(out, "  a beta build, signed by the same key with channel=beta, offered to the stable gate: " + gate(lab, cosign, "2.5.0-beta", List.of("release"), Map.of("channel", "stable")));
        line(out, "  the 'next' key is added to the gate while the build moves over; the gate trusts [release, next]: a release signed by the old key: " + gate(lab, cosign, TAG, List.of("release", "next"), Map.of()));
        cosign.sign("next", release, Map.of());
        line(out, "  the same digest signed with the next key: " + gate(lab, cosign, TAG, List.of("next"), Map.of()));
        line(out, "  the old key is retired: a gate that trusts only [next] and a digest signed only by 'release': " + gate(lab, cosign, "2.5.0-beta", List.of("next"), Map.of()));
        String late = lab.push(Lab.THIEF_IMAGE, APP, "9.9.9");
        cosign.sign("old", late, Map.of());
        line(out, "  a thief with the retired 'old' key signs a new image today: a gate that still trusts 'old': " + gate(lab, cosign, "9.9.9", List.of("old"), Map.of()) + "  <- A SIGNATURE CARRIES NO TIME; WITHOUT A LOG THERE IS NO REVOCATION DAY");
        line(out, "  a gate that dropped 'old': " + gate(lab, cosign, "9.9.9", List.of("release", "next"), Map.of()));
    }

    private static void act4Attestations(StringBuilder out, Lab lab, Cosign cosign) throws IOException, InterruptedException {
        line(out, "FOUR. Signed statements about how the image was built.");
        String release = lab.resolve(APP, TAG);
        Path predicate = Files.writeString(lab.keys().resolve("provenance.json"), "{\"builder\":{\"id\":\"" + CI + "\"},\"source\":\"git+https://git.shop.example/shop@abc123\",\"reproducible\":false}");
        line(out, "  the CI system signs a provenance statement for the digest: the builder is " + CI + ", the source is a commit: " + (cosign.attest("release", release, "slsaprovenance", predicate).ok() ? "attested" : "failed"));
        Cosign.Result good = cosign.verifyAttestation("release", release, "slsaprovenance");
        Map<String, Object> claims = good.ok() ? Cosign.predicateOf(good.output()) : Map.of();
        line(out, "  the gate verifies the attestation with the release key: " + good.reason() + ", and reads the builder: " + ((Map<?, ?>) claims.getOrDefault("builder", Map.of())).get("id"));
        line(out, "  the gate's own rule, builder must be the CI system: " + (CI.equals(((Map<?, ?>) claims.getOrDefault("builder", Map.of())).get("id")) ? "accepted" : "refused"));
        Path laptop = Files.writeString(lab.keys().resolve("laptop.json"), "{\"builder\":{\"id\":\"https://laptop.local\"},\"source\":\"a developer's working copy\"}");
        String other = lab.push(Lab.LAPTOP_IMAGE, APP, "2.4.1");
        cosign.attest("release", other, "slsaprovenance", laptop);
        Map<String, Object> laptopClaims = Cosign.predicateOf(cosign.verifyAttestation("release", other, "slsaprovenance").output());
        line(out, "  an image built on a laptop and attested by the same key: the signature verifies, the builder is " + ((Map<?, ?>) laptopClaims.get("builder")).get("id") + ", and the gate's own rule: "
                + (CI.equals(((Map<?, ?>) laptopClaims.get("builder")).get("id")) ? "accepted" : "refused") + "  <- A SIGNATURE SAYS WHO SIGNED, THE RULE SAYS WHAT WAS SIGNED");
        String forged = lab.push(Lab.THIEF_IMAGE, APP, "forged");
        cosign.attest("attacker", forged, "slsaprovenance", predicate);
        line(out, "  an image whose provenance only the attacker's key signed, with the CI system named as builder: the gate, which trusts the release key: " + cosign.verifyAttestation("release", forged, "slsaprovenance").reason());
        line(out, "  the release key's attestation of one image, offered for another digest: " + cosign.verifyAttestation("release", lab.push(Lab.UNSIGNED_IMAGE, APP, "unsigned"), "slsaprovenance").reason());
        line(out, "  the public transparency log is left out here, because this lab is local; with it, every signature would also be recorded where a monitor can read it");
    }

    private static void act5Bill(StringBuilder out, Lab lab, Cosign cosign) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        String unsigned = lab.resolve(APP, "unsigned");
        line(out, "  the gate refuses an unsigned image: " + gate(lab, cosign, "unsigned", List.of("release"), Map.of()));
        line(out, "  but it protects only the paths that pass through it: a pull straight from the registry gets the unsigned image: " + (lab.resolve(APP, "unsigned").equals(unsigned)));
        line(out, "  the signing key is the crown jewel: with a wrong passphrase it cannot sign: " + (cosign.sign("release", "wrong-passphrase", lab.resolve(APP, TAG), Map.of()).ok() ? "signed" : "refused"));
        line(out, "  rotation is a project: every gate must trust the new key before the old one is dropped, and nothing marks the day an old signature was made");
        line(out, "  keyless signing replaces long-lived keys with short-lived certificates tied to an identity and recorded in a public log; it needs the public Fulcio and Rekor services, which a local lab does not have");
        line(out, "  cosign 3 is moving offline signing behind a signing config, so the flags used here may change");
    }
}
