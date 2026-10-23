# Spec — Security Architecture Review

## Purpose

Teach security architecture review by checking a bank's design in the Structurizr DSL with rules and decision records, and by scoring the rules on planted weaknesses.

## Not in scope

- Drawing tools and diagrams.
- Threat modelling workshops.
- Reviewing code.

## The problem

Design reviews depend on who is in the room, and the decisions made there are not written down or checked later.

## The pieces

| Role | Class |
| --- | --- |
| The design from the DSL | `Design` |
| The decision records | `Adr` |
| The review rules | `Rules` |
| What a rule reports | `Finding` |
| The planted weaknesses | `Planted` |
| Entry point | `ArchitectureReviewDemo` |

## Requirements

1. The design is read from a Structurizr DSL file with zone and flow properties.
2. Rules report findings by rule and subject.
3. Planted weaknesses score the rules, including one that no rule can find.
4. Decision records are read with their status.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 30 lines, identical on every run |
| Acts | 5 |
| Video | 05:34, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
