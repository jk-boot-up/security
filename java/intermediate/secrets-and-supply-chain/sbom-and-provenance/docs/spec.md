# Spec — SBOMs and Build Provenance: What Is Inside, Who Built It, and From What

## Purpose

Teach the difference between what a release contains and how it was made, by writing a CycloneDX SBOM, signing SLSA provenance in a DSSE envelope, checking it at a
deployment gate, explaining SLSA build levels, and making the build reproducible.

## Not in scope

- Scanning SBOMs against advisories: the -with-syft-and-grype version.
- Keyless signing and public transparency logs: the artifact-signing project and its -with-cosign version.
- Running a real build platform.

## The problem

Teams sign releases and publish SBOMs, but deploy without checking where a release was built from, so a valid signature over a fork's build is accepted.

## The pieces

| Role | Class |
| --- | --- |
| A library and its purl | `Component` |
| The bill of materials | `Sbom` |
| The source to build | `Source` |
| Reproducible archives | `Archive` |
| The provenance statement | `Provenance` |
| The signing envelope | `Dsse` |
| The build service | `Builder` |
| The deployment gate | `Policy` |
| SLSA levels | `Pipeline` |
| Entry point | `ProvenanceDemo` |

## Requirements

1. The SBOM is CycloneDX 1.6 JSON with purls and SHA-256 hashes, matches its jar, and finds a changed library.
2. The DSSE encoding matches the specification's example, and the signature covers the payload type.
3. The gate refuses each wrong fact with its reason, and deploys only the real release.
4. Pipelines are placed at SLSA build levels from the facts those levels depend on.
5. Reproducible builds give the same digest regardless of file order and clock.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 19, all passing |
| Demo output | 38 lines, identical on every run |
| Acts | 5 |
| Video | 08:05, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
