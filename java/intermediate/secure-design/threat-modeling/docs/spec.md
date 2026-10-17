# Spec — Threat Modeling with STRIDE

## Purpose

Teach threat modeling by drawing an online bank as a data-flow model, generating STRIDE threats, rating and deciding them, and turning mitigations into executable checks.

## Not in scope

- A complete threat catalogue: the rules find common candidates only.
- A formal quantitative risk method: the advanced track covers that.
- Diagram tools: the -with-pytm version.

## The problem

Teams review code but not designs, and when they do model threats, the model is a document that is out of date by the next release.

## The pieces

| Role | Class |
| --- | --- |
| Elements, flows, boundaries | `Element`, `Flow`, `Boundary` |
| The model and its checks | `Model` |
| The bank | `BankModel` |
| STRIDE rules | `StrideRules` |
| Ratings | `Rating` |
| Decisions | `Decision`, `Register` |
| Mitigations as checks | `Checks` |
| Entry point | `ThreatModelDemo` |

## Requirements

1. The model reports flows that skip a boundary, stores without owners and unconnected elements.
2. The STRIDE rules produce a stable, numbered list of candidates.
3. Every threat in the register has a decision, and every mitigation names an existing check.
4. A change that removes a control fails a check, and new threats without decisions are reported.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 17, all passing |
| Demo output | 42 lines, identical on every run |
| Acts | 5 |
| Video | 07:56, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
