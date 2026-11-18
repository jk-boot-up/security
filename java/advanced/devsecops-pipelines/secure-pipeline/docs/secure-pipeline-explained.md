# Secure Pipeline, Explained

## The idea in one sentence

A pipeline you can trust is a build you can repeat, an artifact you can verify and gates that say no when they cannot say yes.

## The 6 acts

### 1. Build it twice

The input is one commit of the bank's ledger library, cloned twice into clean directories and built with gradlew jar two seconds apart. Three settings are tried: fixed (preserveFileTimestamps false, reproducibleFileOrder true), file times (the old settings: times kept, order from the disk), and build path (fixed, plus a build-info.properties file that records where the build ran).
The result, jar 1 against jar 2: fixed gives the same SHA-256 with 5 = 5 entries, 0 entries with other bytes and the same times. File times gives the same entries and bytes, but the times differ, so the SHA-256 differs. Build path gives 6 = 6 entries with 1 entry of other bytes.
Why: with fixed settings every entry has the constant time 1980-02-01 00:00; with file times each entry has the time of its own build. build-info.properties holds builtIn=<checkout 1> in one jar and builtIn=<checkout 2> in the other.
The fix: fixed archive settings (Gradle 9's default) and nothing about the machine in the jar.

### 2. Sign it

The input: bank-ledger.jar, built from commit 7feddf9e3682, which passed all 4 gates, and a key pair made in a temporary directory for this run. The key in cosign.pub is EC, 256 bits: the curve P-256.
How the signature is made: cosign sign-blob takes the SHA-256 of all the bytes of the jar, signs that digest with ECDSA and cosign.key, and writes the signature in base64 to bank-ledger.jar.sig.
The check: cosign verify-blob, and then SignatureSteps, which redoes it with the JDK alone (SHA256withECDSA and the public key read from cosign.pub). Both say yes for the jar as built and for provenance.json, and no for the jar with one bit changed and for the signature checked with the public key of another key pair.

### 3. Say where it came from

The input: provenance.json, written by the pipeline next to the jar. Its _type is the in-toto Statement v1 and its predicateType is SLSA provenance v1. It names the subject (bank-ledger.jar and its SHA-256), the builder id https://bank.example/securepipe/builder/v1 and the command gradlew jar.
It records 3 inputs: the source (commit 7feddf9e3682, tree a209b03297dd), the build file (build.gradle, sha256 9895030fa19b) and one dependency, pkg:maven/commons-codec/commons-codec@1.17.1.
The checks: the subject equals the SHA-256 of the jar, the commit equals the commit that was checked out, and the tree equals the tree of that commit: yes, yes, yes.
Two that do not match: the previous release names commit 0ada0140f8f3, not this one; and the statement edited to name commit 000000000000 no longer verifies against its signature.

### 4. Four gates, six planted flaws

The input: four gates, in order. The secret scan runs gitleaks over the whole git history and fails on exit 7. The dependency check makes a CycloneDX bill of materials from build.gradle and runs trivy sbom on it, failing on exit 3 for a HIGH or CRITICAL vulnerability. The unit tests run gradlew test, which exits 1 when a test fails. The signature check runs the verifier's 7 checks on the signed release. The clean change passes 4 of 4 and is deployable.
Six flaws are planted: a failing test (the fee is amount / 10, not / 100), a vulnerable library (commons-text 1.9), a committed secret (an access key in a properties file), an unsigned jar, a jar altered after signing, and a jar signed with the attacker's key.
The result: each is stopped at the gate that owns it, after 2, 1, 0, 3, 3 and 3 gates passed: 6 of 6.
What each gate saw: 2 of 4 tests failed (feeIsOnePercentRoundedDown expected 100 and got 1000, because 10000 / 10 = 1000; transferTakesTheFee expected 89900 and got 89000); trivy reported CVE-2022-42889, CRITICAL, in commons-text 1.9; gitleaks reported aws-access-token in config/payments.properties:2; the verifier refused the unsigned jar at files present and the other two at artifact signature.

### 5. A gate whose tool cannot answer

The input: three broken tools, each on a change that must be stopped. gitleaks is not installed, on the change with a committed secret. gitleaks crashes because its configuration is not TOML, on the same change. trivy has an empty database, on the change with the vulnerable library.
What each tool gave back: no program, exit 1 and exit 1. None of them is 0 (pass) or the tool's exit code for found, so the gate's outcome is ERROR every time.
The result, the same errors in two pipelines: the pipeline that fails closed stops at the secret scan, the secret scan and the dependency check: 3 of 3 stopped. The same pipeline set to fail open treats an error as a pass and releases all three flawed changes: 3 of 3 released.

### 6. Deploy only what verifies

The input: the policy deploy holds before it looks at a release: the pipeline's public key cosign.pub, the builder id, and the commit and tree it wants (7feddf9e3682, a209b03297dd). The verifier makes 7 checks in order, and the first that fails refuses: files present, artifact signature, provenance signature, artifact digest, builder, source commit and source tree.
Four releases are offered: the genuine one; forged 1, the jar swapped with the genuine signatures kept; forged 2, an attacker's own build and provenance signed with its own key; forged 3, the genuine previous release, commit 0ada0140f8f3.
The result: genuine 7 of 7, accepted. Forged 1 and forged 2 pass 1 of 7 and are refused at the artifact signature. Forged 3 passes 5 of 7, its signatures and digest are genuine, and is refused at the source commit. 1 jar is in production.
The keys: both key pairs lived in a temporary directory, and no key file is left on disk after the run.

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
