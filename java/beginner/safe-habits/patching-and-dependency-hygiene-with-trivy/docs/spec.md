# Spec — Patching and Dependency Hygiene with Trivy and Gradle

## Purpose

Teach dependency hygiene with real tools by writing a CycloneDX bill of materials, reading a Trivy report and cross-checking it, triaging the findings, and showing that Gradle's verification refuses a changed checksum.

## Not in scope

- Container image scanning, secret scanning and licence checks.
- Running Dependabot or Renovate: their configuration is shown only.
- Signature verification of artifacts.

## The problem

Teams know they should keep dependencies up to date, but not how to find out what they have, what is wrong with it, and what to do first.

## The pieces

| Role | Class |
| --- | --- |
| A Maven library and its package URL | `Dependency` |
| Version comparison | `Version` |
| Lock file reader | `GradleLockfile` |
| CycloneDX writer and checker | `CycloneDx` |
| Trivy report reader | `TrivyReport`, `Vulnerability` |
| Ordering and upgrade targets | `Triage` |
| Cross-check ranges | `OwnAdvisory` |
| Gradle checksums | `VerificationMetadata` |
| Entry point | `PatchingTrivyDemo` |

## Requirements

1. The bill of materials the code writes is CycloneDX 1.6 and matches the committed file.
2. A package URL that disagrees with its version is refused.
3. The report is read, ordered, and one target version is chosen per library.
4. The real Trivy agrees with the code's own checks on the two best known problems (integration test).
5. Gradle refuses to build when a pinned checksum is changed (integration test).
6. The demo needs no tool and prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 20, all passing |
| Demo output | 42 lines, identical on every run |
| Acts | 5 |
| Video | 07:05, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
