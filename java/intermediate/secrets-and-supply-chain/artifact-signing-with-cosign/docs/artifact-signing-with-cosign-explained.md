# Artifact Signing with cosign, Explained

## The idea in one sentence

A deployment should run an image only if a key it trusts signed exactly that digest, with the claims it requires, and everything else is a name that anyone could have moved.

## The 5 acts

### 1. A deployment that runs whatever a tag points at

The input: the release team pushes the image as shop/app:2.4.0 to a real registry running in Docker. The tag 2.4.0 points at digest A, the SHA-256 of the image's manifest, which is a checksum of its content. A pull by tag gets the release.
Then someone with push rights pushes a different image under the same tag. The result: the tag now points at digest B; digest A = digest B is false; a pull by tag gets the other image; a pull by digest A still gets the release.
A digest alone is not enough: the digest itself must come from somewhere that cannot be changed. The demo never prints real digests, which differ between machines; it names them A, B, C in the order it meets them.

### 2. A signed release, checked before it runs

The input: what cosign signs for digest A, read back from cosign verify. Its type is https://sigstore.dev/cosign/sign/v1, it names docker-manifest-digest = digest A and docker-reference = .../shop/app@<digest A>, and it carries no claims.
After signing with the release key, the signature is stored in the same registry as one more tag, sha256-<digest A>. The tag 2.4.0 itself is not signed: the payload names the digest.
The gate pins release.pub, resolves the tag to a digest, and verifies. The tag on digest A: accepted, signed by the release key. The tag moved to digest B: refused, no signature for this digest. Digest B signed with the attacker's own key: refused, not signed by this key. The tag put back on digest A: accepted.

### 3. Signed claims, and keys that rotate

The input: digest A is signed again with the claims channel=stable and version=2.4.0, inside the payload, so they cannot be edited. A beta build, digest C, is signed with channel=beta.
The result: a gate that requires channel=stable accepts digest A; a gate that requires channel=beta refuses it (claims do not match the gate); the stable gate refuses digest C.
The rotation from the release key to next: a gate that trusts both accepts digest A; after digest A is also signed with next, a gate that trusts only next accepts it; digest C, signed only by the release key, is refused by that gate.
Before: a thief with the retired old key signs digest D today, and the signature carries no date. After: a gate that still trusts old accepts it; only a gate that dropped old refuses it. Without a log there is no day to revoke from.

### 4. Signed statements about how the image was built

The input: the provenance the CI system signs for digest A, with the release key, as type slsaprovenance: builder https://ci.shop.example, source git+https://git.shop.example/shop@abc123.
The result, first the signature and then the gate's own rule that the builder must be the CI system: digest A, signature accepted, builder https://ci.shop.example, rule accepted; digest E, a laptop build attested by the same key, signature accepted, builder https://laptop.local, rule refused. A signature says who signed; the rule checks what was signed.
A provenance signed by the attacker's key is refused (not signed by this key), and the release key's attestation is not valid for another digest, F (no signature for this digest). The public transparency log is left out, because this lab is local.

### 5. The bill

The gate refuses the unsigned image, digest F, with no signature for this digest, but a direct pull from the registry gets it anyway: the gate protects only the paths that pass through it.
The signing key is the crown jewel: with a wrong passphrase it cannot sign. Rotation is a project: every gate must trust the new key before the old one goes, and nothing marks the day an old signature was made.
Keyless signing replaces long-lived keys with short-lived certificates tied to an identity and logged in public; it needs the Fulcio and Rekor services, which a local lab does not have. cosign 3 is moving offline signing behind a signing config, so the flags used here may change.

## The verdict

Deploy by digest, verify a signature from a pinned key on that digest, require signed claims and provenance and check them with your own rules, plan key rotation, keep keys in hardware, log every signature, and make the gate the only way an image starts.

## How to recognise this in code you did not write

- `cosign verify` or `cosign sign` in a pipeline.
- A `sha256-...sig` tag in a registry.
- `image: name@sha256:...` in a deployment.
- An in-toto statement with a `predicateType`.

## Where you have already met this

- `image: shop/app:latest` in a deployment file.
- `cosign verify` in a pipeline or an admission policy.
- A `.sig` tag next to an image in a registry.
- A provenance file named `provenance.intoto.jsonl`.
