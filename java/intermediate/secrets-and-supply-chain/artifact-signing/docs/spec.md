# Spec — Artifact Signing: Signed Releases, Key Rotation and Transparency Logs

## Purpose

Teach how to trust a release by signing a statement over its digest, verifying it with pinned keys per channel, rotating and revoking keys, and requiring every signature to
be in a public, append-only Merkle log that the publisher monitors.

## Not in scope

- Container image signing formats and registries: the -with-cosign version.
- Build provenance and SBOMs: the sbom-and-provenance project.
- Hardware security modules: described, not run.

## The problem

Teams verify downloads with a checksum from the same place as the file, keep one signing key forever, trust the signer's dates, and have no way to see a signature made in secret.

## The pieces

| Role | Class |
| --- | --- |
| The release statement | `Statement` |
| Signing keys | `SigningKey` |
| Trusted keys and revocation | `KeyRing` |
| Checks before deployment | `Verifier` |
| The Merkle tree and its proofs | `MerkleTree` |
| The public log | `TransparencyLog` |
| The store's watcher | `Monitor` |
| The point of use | `DeployGate` |
| Entry point | `SigningDemo` |

## Requirements

1. A changed jar, an edited statement, a forged key, an unknown key and a missing signature are each refused with their reason.
2. A key signs only for its channel; keys overlap during rotation, and revocation counts from its day.
3. Ed25519 matches RFC 8032 test 1, and every inclusion and consistency proof checks for logs of up to seventeen entries.
4. The gate refuses a signature that is not in the log, and revokes by the log's day.
5. The monitor reports only unknown releases signed with the store's keys, and two heads of one size with different roots prove a fork.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 21, all passing |
| Demo output | 40 lines, identical on every run |
| Acts | 5 |
| Video | 08:35, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
