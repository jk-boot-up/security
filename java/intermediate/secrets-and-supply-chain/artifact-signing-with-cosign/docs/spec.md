# Spec — Artifact Signing with cosign

## Purpose

Teach signing and verifying container images with cosign: digests, keys, claims and attestations.

## Not in scope

- Keyless signing with the public Sigstore services.
- Running a transparency log.
- Admission controllers.

## The problem

Teams deploy by tag from a registry that several parties can write to, and have no proof that what runs is what was built.

## The pieces

| Role | Class |
| --- | --- |
| The lab | `Lab` |
| The tool | `Cosign` |
| Entry point and gate | `CosignSigningDemo` |

## Requirements

1. A gate resolves a tag to a digest and verifies a signature over that digest from a pinned key.
2. A moved tag, an untrusted key and an unsigned image are refused.
3. Signed claims must match what the gate requires.
4. A gate that still trusts a retired key accepts a signature made today.
5. A provenance statement is verified and then checked against the gate's own rule.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 9, all passing |
| Demo output | 39 lines, identical on every run |
| Acts | 5 |
| Video | 06:14, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
