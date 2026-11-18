# SBOMs and Build Provenance: What Is Inside, Who Built It, and From What, Explained

## The idea in one sentence

An SBOM records what a release contains, signed provenance records which builder made it from which repository, branch and commit, and a deployment should check both,
with reproducible builds letting anyone confirm the result.

## The 5 acts

### 1. What a bill of materials answers, and what it does not

The input is the release shop-1.4.0.jar: three files (META-INF/MANIFEST.MF, com/shop/Store.class, com/shop/Checkout.class) and three libraries, acme-http 3.0.2,
acme-json 2.4.1 and acme-log 1.9.0. The CycloneDX 1.6 SBOM records the application with the jar's SHA-256 (38a6dd0dd912fc89...) and each library by its package URL
with the SHA-256 of its jar. One entry worked out: acme-json's package URL is pkg:maven / com.acme / acme-json @ 2.4.1, and the 22 bytes of its jar hash to
2843abbaa80d801d.... Checking: the SBOM's application hash equals the jar's, so it is the SBOM for this jar; acme-json on disk now hashes to d82288d74b4b8218...,
not the recorded value, so it was changed after the SBOM was written. The repository, branch and commit, and who built the jar, are not in the SBOM: that is provenance.

### 2. Provenance: who built it, from what

The input is the statement the build service writes: _type https://in-toto.io/Statement/v1, the subject shop-1.4.0.jar with SHA-256 38a6dd0dd912fc89..., the predicate
type https://slsa.dev/provenance/v1, the build type, the repository https://git.shop.example/shop/store, the ref refs/heads/main, the commit 4f1c2a9e8b7d..., the
builder https://build.shop.example/runner/v1 and the invocation build-1042.
How the signed bytes are made: DSSE's pre-authentication encoding is DSSEv1, the length of the type in bytes, the type, the length of the body in bytes, and the
body. The type application/vnd.in-toto+json is 28 bytes and the statement 768, so the signed bytes start "DSSEv1 28 application/vnd.in-toto+json 768 {".
The result is an envelope: the payload type, the payload in base64 (1024 characters), the key id shop-ci-2026 and a 64-byte Ed25519 signature, which verifies with
the builder's key. The same bytes offered as application/json are signed bytes that start "DSSEv1 16 application/json 768", over which the signature is false, and
the gate refuses the type before it even tries.

### 3. Checking before deploying

The input is the gate's policy: the key shop-ci-2026, the builder https://build.shop.example/runner/v1, the repository https://git.shop.example/shop/store and the
ref refs/heads/main; and its seven checks, in order: provenance, signature, statement, subject, builder, repository, branch. Eight releases are offered.
The result: A, the real release, passes all seven checks and is deployed. B, a fork, fails check 6 (another repository); C, an unreviewed branch, check 7; D, a
laptop's key, check 2 (no signature from a trusted builder); E, the trusted key naming the staging runner, check 5; F, the fork's statement edited to name main's
repository, check 2; G, the fork's jar with the real provenance, check 4; H, no provenance, check 1.
How two refusals are made: the fork's signed statement is 772 bytes and names the fork; the edited one is 768 bytes and names main's repository, and Ed25519 over the
edited bytes is false. The real statement's subject is sha256 38a6dd0dd912fc89..., but the fork's jar is 6d76d2c9bc23cc20..., so the statement is about another artifact.

### 4. SLSA build levels, and builds that can be repeated

The input is four pipelines and what each does: the laptop writes no provenance; the signed laptop writes it but the developer signs it; the hosted CI runner has the
platform sign it, but the key is not isolated from the build steps; the hardened platform isolates it. The result: levels 0, 1, 2 and 3, held back by "no provenance",
"the developer holds the key" and "build steps can reach the key".
Two careless builds of the same commit, at 2026-10-01 09:03:12 and 2026-10-02 14:40:51, with the files in the order the file system listed them, give jars
2f40929e19141de8... and abed690ac267ede2...: not the same. Two reproducible builds, both stamped with the commit time 2026-10-01 09:00:00 and with entries sorted by
name, both give 38a6dd0dd912fc89..., the provenance subject, so the gate deploys the rebuild.

### 5. The bill

Without a gate, a deploy script runs the fork's build. With the builder's key stolen, a statement whose subject is the fork's jar (6d76d2c9bc23cc20...) and whose
repository is the store's, signed with shop-ci-2026, passes every check and is deployed: the key and the builder must be protected. The SBOM written for 1.4.0
records sha256 38a6dd0dd912fc89..., while the 1.4.1 jar is f3710cc65bf5ecd1..., so it does not describe it: write the SBOM at every build. And a correct SBOM still
needs scanning against new advisories, which is the -with-syft-and-grype version.

## The verdict

Write an SBOM at every build and scan it continuously. Have the build platform, not developers, sign provenance, and check it at deployment against the expected
builder, repository and branch. Protect the builder's key and isolate builds from it. Make builds reproducible so that independent rebuilds can confirm the provenance.

## How to recognise this in code you did not write

- Release pages that publish an SBOM and a provenance file next to each artifact.
- Admission policies that require provenance from a named builder.
- Questions from customers or regulators asking for an SBOM.
- SLSA levels in project documentation and scorecards.

## Where you have already met this

- CycloneDX and SPDX files attached to releases and requested by customers and regulators.
- `*.intoto.jsonl` and `*.sigstore.json` files next to release downloads.
- SLSA levels mentioned in project security policies and OpenSSF Scorecard checks.
- Supply chain incidents where a build server was compromised and signed releases were shipped.
