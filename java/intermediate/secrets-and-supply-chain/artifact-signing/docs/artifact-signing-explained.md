# Artifact Signing: Signed Releases, Key Rotation and Transparency Logs, Explained

## The idea in one sentence

A deployment should run only an artifact whose exact bytes match a statement signed by a trusted key for its channel, and whose signature is in an append-only public log
that the publisher monitors, so that changed files, edited statements, forged keys, secret signatures and backdated signatures are all refused or found.

## The 5 acts

### 1. A deployment that runs whatever arrives

The release team builds shop-app-2.4.0.jar, 2440 bytes with SHA-256 4577e6b10a2f145d. The deployment downloads it from a mirror where one byte was changed, so its
SHA-256 is 6d6cca4f654956e0. The trusting deployment step deploys it without checking: an altered jar is running. A digest published next to the file does not help,
because whoever can change the file can change the digest beside it.

### 2. A signed release, checked before it runs

The release statement names the artifact, its version, its channel, the SHA-256 of its exact bytes and the signing day. It is signed with the key stable-2026a, giving a
64-byte Ed25519 signature that is the same on every run. The deployment pins that key's public key and checks before it deploys. The real jar is accepted. The jar with one
byte changed is refused because it does not match the signed digest. The statement edited to version 2.4.1, with the signature kept, is refused. A signature from another key
that calls itself stable-2026a is refused, a key the deployment does not know is refused, and no signature at all is refused.

### 3. Signing keys: per channel, rotated, revoked

A beta build signed with beta-2026a and offered to the stable deployment is refused: that key signs for beta, not stable. On day 100 stable-2026b is added, and both stable
keys are trusted while the build moves over, so releases signed on day 110 with either key are accepted. On day 130 stable-2026a is revoked. A release it signed on day 120,
before the revocation, is still accepted, and one it signed on day 140 is refused. But the signer writes the day: a thief with the old key signs a different jar on day 150,
dates it day 120, and it is accepted. A backdated signature passes, because nothing independent records when it was really made.

### 4. A public log that every signature must be in

The store logs its releases, 2.3.0 on day 90 and 2.4.0 on day 120, in an append-only log whose tree head (size 2, root c1e815c249fdd097) is signed by the log. The proof that
2.4.0 is in the log is one hash, and it checks against the signed head. The deployment now requires a log entry and accepts the real release as entry 1. The thief's backdated
jar is refused when it is not in the log, and refused when it is, because the log recorded it on day 150, after the revocation: the log's day counts, not the signer's.
The store's monitor reads the log and reports entry 2 as a version 2.4.0 signed with stable-2026a that is not a release the store made. A log operator who hides that entry
from the monitor must show two views, both signed, both of size 3, with different roots. Each still extends the earlier head of size 2, but two signed heads of the same
size with different roots cannot both be true, so comparing heads proves the fork.

### 5. The bill

The gate refuses an unsigned jar, but it protects only the paths that pass through it: a jar copied straight onto a server never meets the gate. The signing key is the crown
jewel: in the demo it comes from a fixed seed, and in practice it stays in a hardware module, with each signing approved and logged. Rotation is a project, because every
deployment must trust the new key before the old one is revoked, here with an overlap of 30 days. Keyless signing replaces long-lived keys with short-lived certificates tied
to an identity and recorded in a public log, which is the subject of the -with-cosign version.

## The verdict

Sign every release statement over the artifact's digest and verify it at the point of use, with keys pinned per channel. Keep signing keys in hardware, rotate them with an
overlap, and revoke by an independent time. Require signatures to be in a transparency log, and monitor it for releases you did not make.

## How to recognise this in code you did not write

- Package managers that refuse a package whose signature does not verify.
- Admission rules that only allow signed container images to run.
- Signed tree heads and inclusion proofs in Certificate Transparency logs.
- Security findings titled "download of code without integrity check", CWE-494.

## Where you have already met this

- Signed jars, signed container images and signed operating system packages that a package manager checks before installing.
- `.sig` and `.asc` files next to downloads, and a published public key or fingerprint.
- Certificate Transparency, which logs every web certificate in public Merkle logs.
- Supply chain incidents where a build server or a mirror served a changed release.
