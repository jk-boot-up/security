# Spec — SBOMs and Scanning with syft and grype

## Purpose

Teach cataloguing and scanning container images with syft and grype, and gating on the result.

## Not in scope

- Signing the bill or the build provenance.
- Running a vulnerability database of your own.
- Exploitability analysis.

## The problem

Teams do not know what their images contain, and learn of a critical advisory from the news.

## The pieces

| Role | Class |
| --- | --- |
| The lab | `Lab` |
| The bill | `Sbom` |
| The scan | `Scan` |
| Entry point and gate | `SbomScanDemo` |

## Requirements

1. A bill of materials is written for an image by syft and read back.
2. A scan of the image and of the bill alone find the same advisories.
3. A gate refuses critical and high advisories in the store's libraries.
4. An exception covers a high advisory only until its end day, and never a critical one.
5. Nothing that depends on the day's database is printed.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 30 lines, identical on every run |
| Acts | 5 |
| Video | 05:47, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
