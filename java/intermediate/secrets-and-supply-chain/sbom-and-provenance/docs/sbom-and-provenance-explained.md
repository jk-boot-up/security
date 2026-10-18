# SBOMs and Build Provenance: What Is Inside, Who Built It, and From What, Explained

## The idea in one sentence

An SBOM records what a release contains, signed provenance records which builder made it from which repository, branch and commit, and a deployment should check both,
with reproducible builds letting anyone confirm the result.

## The 5 acts

### 1. What a bill of materials answers, and what it does not

The release shop-1.4.0.jar is built from three files and depends on three libraries. Its CycloneDX 1.6 SBOM lists them by package URL, each with a SHA-256:
acme-http 3.0.2, acme-json 2.4.1 and acme-log 1.9.0. The SBOM's application hash matches the jar, so it is the SBOM for this jar, and a library jar changed after
the SBOM was written is found by its hash. But which repository, branch and commit the jar was built from, and who built it on which machine, the SBOM does not say.
That is what provenance is for.

### 2. Provenance: who built it, from what

The build service writes an in-toto statement with the predicate type https://slsa.dev/provenance/v1. Its subject is shop-1.4.0.jar with SHA-256 38a6dd0dd912fc89.
The build definition names the build type, the source repository https://git.shop.example/shop/store, the branch refs/heads/main and the commit 4f1c2a9e8b7d. The run
details name the builder, https://build.shop.example/runner/v1. The statement is signed with Ed25519 in a DSSE envelope, over the encoding "DSSEv1 28
application/vnd.in-toto+json 768" followed by the statement, and verifies with the builder's key shop-ci-2026. Because the payload type is part of what is signed, the
same bytes offered as another payload type are refused.

### 3. Checking before deploying

The deployment gate trusts one builder key and one builder id, and expects the main branch of the store's repository. The release from the trusted builder, built from
main, is deployed. A build of a fork is refused because it was built from another repository, and a build of an unreviewed branch because it was built from another
branch. A build signed on a developer's laptop has no signature from a trusted builder. A statement signed with the trusted key but naming the staging runner names a
builder that is not trusted. The fork's statement edited after signing to say main's repository no longer matches its signature. The fork's jar offered with the real
release's provenance is refused because the statement is about another artifact. And a jar with no provenance at all is refused.

### 4. SLSA build levels, and builds that can be repeated

SLSA build levels are defined by what each adds. A developer's laptop build is level zero: it writes no provenance. A laptop build that signs its own provenance is level
one, held back because the developer holds the key and could forge it. The store's hosted CI runner is level two, held back because build steps run where the signing key
can be reached, so a malicious build could sign anything. A hardened build platform with isolated, short-lived builds is level three. Two careless builds of the same
commit, each with its own clock and the file system's order, give different jars. Two reproducible builds, with the commit time and sorted entries, give the same jar,
SHA-256 38a6dd0dd912fc89, so anyone can rebuild the commit and check it against the provenance subject.

### 5. The bill

Provenance only helps when it is checked: a deploy script that never asks runs the fork's build. With the builder's key stolen, a fork's jar with a statement
claiming main is deployed by the gate, so the key and the builder must be protected. The SBOM written for 1.4.0, checked against the 1.4.1 jar, does not describe it, so
the SBOM must be written at every build. And a correct SBOM is only useful if something scans it against new advisories, which is the -with-syft-and-grype version.

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
