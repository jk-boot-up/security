# Spec — Secure Pipeline

## Purpose

Teach a release pipeline with reproducible builds, signatures, provenance and gates that fail closed, measured on planted failures and forgeries.

## Not in scope

- Running a Gitea and Woodpecker server.
- Keyless signing and a transparency log.
- Securing the builder itself.

## The problem

Releases are built wherever, checked when convenient and trusted without proof.

## The pieces

| Role | Class |
| --- | --- |
| The stages and gates | `Pipeline`, `Gate` |
| Building twice and comparing | `Reproducibility` |
| Signing with cosign | `Cosign` |
| The provenance statement | `Provenance` |
| The bill of materials | `Sbom` |
| Everything checked before trust | `Verifier` |
| The step before production | `Deploy` |

## Requirements

1. The same commit gives identical jars in two directories, and the variants that do not are shown.
2. The artifact and its provenance are signed and verified with a key made for the run.
3. The provenance subject equals the jar and the source equals the checked-out commit.
4. Each planted failure stops the pipeline at its own gate.
5. A tool that is missing or crashes stops the pipeline.
6. Deploy accepts only a release that verifies and refuses forged ones.
7. The demo prints the same output on every run and stores no key.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 20, all passing |
| Demo output | 48 lines, identical on every run |
| Acts | 5 |
| Video | 06:23, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
