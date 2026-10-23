# Spec — SIEM Basics

## Purpose

Teach the basics of a SIEM by parsing, normalizing and correlating three bank log sources.

## Not in scope

- Operating a SIEM product.
- Scaling ingestion.
- Machine-learning detections.

## The problem

Attacks cross systems while logs are kept in separate formats and clocks, so each source looks innocent.

## The pieces

| Role | Class |
| --- | --- |
| The one event shape | `Event` |
| The log lines | `Sources` |
| The parsers | `Parsers` |
| The rules and cases | `Correlator` |
| Entry point | `SiemBasicsDemo` |
| The tests | `SiemTest` |

## Requirements

1. Three formats are parsed into one event shape in UTC.
2. Lines that do not parse are kept and counted.
3. Ignoring a source's offset changes a rule's result as the demo prints.
4. A rule over the joined events finds an attack that no single source shows.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 10, all passing |
| Demo output | 22 lines, identical on every run |
| Acts | 5 |
| Video | 05:34, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
