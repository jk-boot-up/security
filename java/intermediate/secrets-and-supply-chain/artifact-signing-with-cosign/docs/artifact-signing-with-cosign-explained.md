# Artifact Signing with cosign, Explained

## The idea in one sentence

A deployment should run an image only if a key it trusts signed exactly that digest, with the claims it requires, and everything else is a name that anyone could have moved.

## The 5 acts

### 1. A deployment that runs whatever a tag points at

The release team pushes the image as shop/app:2.4.0 and the tag points at one digest. The deployment pulls by tag and gets exactly that image. Someone with push rights, or a registry that is not what it was, pushes a different image under the same tag: the digests differ. The deployment pulls by tag again and gets the other image: a tag is a name, not a checksum. Pulling by the digest it was given gets the release, but only if the digest itself came from somewhere that cannot be changed.

### 2. A signed release, checked before it runs

The release pipeline signs the image digest with the release key. The signature is stored in the same registry, next to the image, as one more tag. The deployment pins the public key and checks before it deploys: accepted. The tag is moved to the attacker's image and the same gate refuses it: no signature for this digest. The attacker signs their own image with a key of their own and the gate refuses that too: not a key it trusts. The tag is put back and the gate accepts.

### 3. Signed claims, and keys that rotate

The release signs the digest again with the claims channel=stable and version=2.4.0. A gate that requires channel=stable accepts; one that requires channel=beta refuses. A beta build signed by the same key with channel=beta, offered to the stable gate, is refused. The next key is added while the build moves over, so the gate trusts the release and next keys, and both signatures are accepted. When the old key is retired, a gate that trusts only the next key refuses a digest signed only by the old one. A thief with the retired old key signs a new image today and a gate that still trusts the old key accepts it: a signature carries no time, and without a log there is no revocation day. A gate that dropped the old key refuses.

### 4. Signed statements about how the image was built

The CI system signs a provenance statement for the digest: the builder is the CI system and the source is a commit. The gate verifies the attestation with the release key and reads the builder; its own rule, that the builder must be the CI system, is satisfied. An image built on a laptop and attested by the same key verifies, the builder is the laptop, and the gate's own rule refuses it: a signature says who signed, the rule says what was signed. An image whose provenance only the attacker's key signed, naming the CI system as builder, is refused by a gate that trusts the release key. The release key's attestation offered for another digest is refused. The public transparency log is left out because this lab is local.

### 5. The bill

The gate refuses an unsigned image. But it protects only the paths that pass through it: a pull straight from the registry gets the unsigned image. The signing key is the crown jewel: with a wrong passphrase it cannot sign. Rotation is a project: every gate must trust the new key before the old one is dropped, and nothing marks the day an old signature was made. Keyless signing replaces long-lived keys with short-lived certificates tied to an identity and recorded in a public log; it needs the public Fulcio and Rekor services, which a local lab does not have. Cosign 3 is moving offline signing behind a signing config, so the flags used here may change.

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
