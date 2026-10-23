# Secure Pipeline, Explained

## The idea in one sentence

A pipeline you can trust is a build you can repeat, an artifact you can verify and gates that say no when they cannot say yes.

## The 5 acts

### 1. Build it twice

The library is cloned twice into clean directories and built with Gradle. With preserveFileTimestamps off and reproducibleFileOrder on, the two jars have the same SHA-256. With the old settings (file times kept) they differ, though the entries are the same and no content differs. With a build-info file that records the build path, one entry (build-info.properties) differs. The fix is to fix the archive settings and to put nothing about the machine in the jar. Gradle 9 does the first by default.

### 2. Sign it

A cosign key pair is made in a temporary directory. The jar and the provenance are signed with cosign sign-blob, and cosign verify-blob checks them with the public key. A jar with one bit changed fails. A signature checked against the public key of another key pair fails. The key files are deleted at the end of the run.

### 3. Say where it came from

The provenance names the builder id, the build command (gradlew jar), 3 inputs (the source commit and tree, build.gradle and 1 dependency), and the SHA-256 of the jar as its subject. The verifier checks that the subject digest equals the jar, that the source commit equals the commit that was checked out, and that the tree matches. The provenance of the previous release names another commit, and an edited provenance no longer verifies.

### 4. Gates that fail closed

The gates are a secret scan (gitleaks over the whole history), a dependency check (trivy on a CycloneDX bill of materials made from build.gradle), the unit tests, and a signature check. Planted: a failing test, a vulnerable dependency (commons-text 1.9), a committed secret built from fragments, an unsigned artifact, a jar altered after signing and a jar signed with an unknown key. 6 of 6 are stopped and each at the gate that owns the flaw. Then tools are broken: gitleaks missing, gitleaks crashing on its configuration and trivy without its database. A pipeline that fails closed stops in 3 of 3; one that fails open releases 3 of 3.

### 5. Deploy only what verifies

Deploy verifies the signatures of the jar and the provenance, the digest, the builder and the source commit and tree before it copies anything. The genuine release is accepted. A jar swapped after signing is refused at the artifact signature. An attacker's own build with a matching provenance, signed with the attacker's key, is refused at the artifact signature. The genuine previous release, replayed, is refused at the source commit. One jar is in production.

## The verdict

Build in clean directories with fixed archive settings and compare the digests, sign the artifact and its provenance, verify both again at deploy together with the commit, and make every gate fail when its tool does not answer.

## How to recognise this in code you did not write

- A green pipeline where a scanner was never installed.
- A jar with no record of its source.
- A build that differs on every run.
- A check that is skipped when the tool is missing.

## Where you have already met this

- A pipeline that is green because the scanner was not installed.
- A jar on a shared drive that nobody can trace to a commit.
- A hotfix built on a laptop.
- An old release that was valid yesterday and is replayed today.
