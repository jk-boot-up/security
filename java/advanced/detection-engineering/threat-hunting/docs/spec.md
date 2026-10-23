# Spec — Threat Hunting

## Purpose

Teach threat hunting by running four hypothesis-driven hunts over a seeded week of bank logs and scoring the leads.

## Not in scope

- Tools for hunting at scale.
- Machine-learning anomaly detection.
- Incident response.

## The problem

Teams wait for alerts and do not look for what the alerts miss, or they hunt without a hypothesis and cannot say what they learned.

## The pieces

| Role | Class |
| --- | --- |
| The events | `Events` |
| The seeded week | `Generator` |
| The hunts | `Hunts` |
| A lead | `Hunts.Lead` |
| The scoreboard | `ThreatHuntingDemo` |
| Entry point | `ThreatHuntingDemo` |

## Requirements

1. The week is seeded and has known attacks and known benign look-alikes.
2. Each hunt starts from a hypothesis and gives leads.
3. Leads are judged against the truth and counted.
4. A hunt can be written as a rule and backtested with and without an allowlist.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 9, all passing |
| Demo output | 25 lines, identical on every run |
| Acts | 5 |
| Video | 05:42, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
