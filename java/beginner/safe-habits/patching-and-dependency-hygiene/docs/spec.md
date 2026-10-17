# Spec — Patching and Dependency Hygiene

## Purpose

Teach the habits behind patching by listing a store's components, matching them against fictional advisories, scoring and ranking the findings, tracking fix deadlines on a simulated clock and verifying pinned hashes.

## Not in scope

- A real advisory feed: the tools counterpart uses Trivy and Dependency-Check.
- Call-graph reachability analysis.
- Signature verification of artifacts, which is the signing projects.

## The problem

Teams know they should patch, but cannot say what they ship, which flaws matter, or how fast a fix must be.

## The pieces

| Role | Class |
| --- | --- |
| Inventory | `Sbom`, `Component` |
| Versions and ranges | `SemVer`, `VersionRange` |
| Advisories and matching | `Advisory`, `Audit`, `Finding` |
| Scoring | `Cvss31` |
| Deadlines | `PatchWindow` |
| Pins and lookalikes | `Pins`, `Typosquat` |
| Entry point | `PatchingDemo` |

## Requirements

1. Versions compare as numbers, and ranges include the broken versions and exclude the fixed one.
2. An inventory with dependencies of dependencies is read, with the shortest path to each component.
3. Findings come from matching installed versions against advisories, and the fixed versions are not flagged.
4. The CVSS 3.1 base score matches published examples.
5. Triage puts reachable findings first and then the higher score.
6. A changed or unreviewed artifact is refused.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 37, all passing |
| Demo output | 49 lines, identical on every run |
| Acts | 5 |
| Video | 08:43, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
