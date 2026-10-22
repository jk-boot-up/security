# Spec — Attack Trees and Abuse Cases in Threat Dragon

## Purpose

Teach putting abuse cases into a Threat Dragon model file, validating it, and keeping it true.

## Not in scope

- Drawing in the editor.
- Running Threat Dragon as a service.
- Other model formats.

## The problem

Threats written in a document are far from the flows they concern, and nobody knows which are still open.

## The pieces

| Role | Class |
| --- | --- |
| The tree and the plan | `BankTree`, `Planner`, `AbuseCase` |
| The placement | `Cases` |
| The model file | `ModelBuilder` |
| The checks | `ModelChecks` |
| The image | `Dragon` |
| Entry point | `ThreatDragonDemo` |

## Requirements

1. The model is written with stable identifiers.
2. The real validator of the real image accepts it and refuses broken variants.
3. Open threats in the file equal the failing abuse cases at every step of the plan.
4. Rules find what the schema cannot.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 45 lines, identical on every run |
| Acts | 5 |
| Video | 05:48, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
