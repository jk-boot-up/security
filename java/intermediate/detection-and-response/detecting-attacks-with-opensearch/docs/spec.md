# Spec — Detecting Attacks with OpenSearch

## Purpose

Teach writing, tuning and scheduling detections in a real OpenSearch, and knowing which ones need code.

## Not in scope

- Sizing or running OpenSearch.
- Notification channels and routing.
- Machine-learning detectors.

## The problem

Logs hold attacks that nobody sees, and alerts that nobody trusts are as bad as none.

## The pieces

| Role | Class |
| --- | --- |
| The server | `Lab`, `OpenSearch` |
| The queries | `Detections`, `Hit` |
| The monitors | `Monitors` |
| The scenario and the code rule | `Scenario`, `Event`, `ImpossibleTravelRule` |
| Entry point | `OpenSearchDetectDemo` |

## Requirements

1. The day is loaded under an explicit mapping.
2. Each detection is a query with a filter, a subject, a time bucket and a threshold.
3. The tuned detections run as monitors of the alerting plugin, once with dryrun.
4. Detections a query cannot express are done in code from the events read back.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 19, all passing |
| Demo output | 46 lines, identical on every run |
| Acts | 5 |
| Video | 06:05, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
