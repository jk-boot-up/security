# Artifact Signing: Signed Releases, Key Rotation and Transparency Logs, Explained

## The idea in one sentence

A deployment should run only an artifact whose exact bytes match a statement signed by a trusted key for its channel, and whose signature is in an append-only public log
that the publisher monitors, so that changed files, edited statements, forged keys, secret signatures and backdated signatures are all refused or found.

## The 5 acts

### 1. A deployment that runs whatever arrives

The input is the jar the release team built: shop-app-2.4.0.jar, 2440 bytes, byte 700 is '/' (0x2f), and its SHA-256 starts 4577e6b10a2f145d.
The mirror flips one bit: byte 700 becomes '.' (0x2e), and the SHA-256 becomes 6d6cca4f654956e0, with every hex digit different.
The result: the trusting deployment step accepts it and deploys shop-app-2.4.0.jar without checking it, so an altered jar is running. A digest published next to the file does not help, because whoever can change the file can change the digest beside it.

### 2. A signed release, checked before it runs

The input is the release statement, 151 bytes of text in one exact form: release-statement/v1, name=shop-app, version=2.4.0, channel=stable, the full 64-hex-digit SHA-256 of the jar, and signed-day=120.
The result of signing: an Ed25519 signature of 64 bytes, starting 6caf4ae3bad1f0aa, by the key stable-2026a, whose 32-byte public key starts 797d4b479b52e839. Signing again gives the same signature, because Ed25519 is deterministic.
The deployment pins that public key and checks in order: 1 the statement parses in its exact form, 2 the key id is pinned, 3 the Ed25519 signature verifies, 4 the key and the statement are for the stable channel, 5 it was signed while the key was trusted, 6 the SHA-256 of the jar equals the signed one.
The results: the real jar is accepted; the jar with one byte changed is refused at check 6; the statement edited to 2.4.1 with the signature kept, and a forged key that calls itself stable-2026a, are refused at check 3 (bad signature); an unknown key, attacker-1, is refused at check 2; no signature is refused at once.

### 3. Signing keys: per channel, rotated, revoked

A beta build signed with beta-2026a and offered to the stable deployment is refused: beta-2026a signs beta, not stable.
The rotation: stable-2026a is trusted from day 0 and revoked on day 130; stable-2026b is added on day 100. Both are trusted for 130 - 100 = 30 days, day 100 to 129.
Signed on day 110 with either key: accepted. Signed on day 120 with stable-2026a: accepted. Signed on day 140 with stable-2026a: refused, signed day 140, revoked day 130.
Before: a thief with stable-2026a signs a different jar (SHA-256 14669a56d76a94fc) on day 150 and writes signed-day=120. After: accepted, because the signer writes the day, and nothing independent records when it really signed.

### 4. A public log that every signature must be in

The input: the log holds entry 0, 2.3.0 by stable-2026a logged day 90, and entry 1, 2.4.0 logged day 120; each entry is the key id, the statement and the signature.
How the root is made, as RFC 9162 defines it: leaf 0 = sha256(00 || entry 0) = 05e76fd5f6c4ca1a, leaf 1 = sha256(00 || entry 1) = cf27ce9d14a8e52d, and root = sha256(01 || leaf 0 || leaf 1) = c1e815c249fdd097. The log signs a head: size 2 and that root.
The proof that entry 1 is in the log is one hash, leaf 0: sha256(01 || proof || leaf 1) gives c1e815c249fdd097 again, which equals the signed root.
The deployment now requires a log entry: the real 2.4.0 is accepted as entry 1; the thief's jar is refused when it is not in the log, and refused when it is, because it was logged on day 150 and the key was revoked on day 130. The store's monitor reports entry 2: 2.4.0 by stable-2026a with SHA-256 14669a56d76a94fc, a release it never made.
A log that hides entry 2 from the monitor must show two views of size 3: to the monitor the beta build, root f230f49e340792fc; to the customer the thief's jar, root 92f28850530761dd. Each is sha256(01 || old root || leaf 2), so each is consistent with the head of size 2, but two signed roots for one size prove the fork.

### 5. The bill

The gate refuses an unsigned jar with the reason no signature, but a jar copied by hand onto a server never meets the gate, so the other paths must be locked.
The signing key is the crown jewel: here it is made from a fixed demo seed; in practice it stays in a hardware module, and each use is approved.
Rotation is a project: every deployment must trust the new key before the old one is revoked, here with an overlap of 30 days.
Keyless signing replaces long-lived keys with short-lived certificates tied to an identity and recorded in a public log, which is the subject of the -with-cosign version.

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
