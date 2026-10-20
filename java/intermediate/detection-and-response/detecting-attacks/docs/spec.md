# Spec — Detecting Attacks in Logs

## Purpose

Teach detection engineering by writing five rules over a synthetic day of sign-in and admin logs, scoring them against ground truth, tuning a threshold, and arriving at a rule set whose alerts an analyst can read.

## Not in scope

- A SIEM, streaming or storage: the detecting-attacks-with-opensearch project.
- Machine-learning detection.
- Responding to alerts: the incident-response project.

## The problem

Logs are kept and never turned into alerts, or turned into so many alerts that nobody reads them.

## The pieces

| Role | Class |
| --- | --- |
| Data | `Event`, `City`, `Scenario` |
| Rules | `Rule`, `BruteForceRule`, `SprayingRule`, `ImpossibleTravelRule`, `TakeoverRule`, `PrivilegeChangeRule` |
| Results | `Alert`, `Score` |
| Entry point | `DetectionDemo` |

## Requirements

1. Each rule fires on its attack in a hand-made log and not on its near misses.
2. The scenario is repeatable and contains four attacks with ground truth.
3. First versions notice the attacks with many false alerts; tuning removes them; a threshold above the attack misses it.
4. The tuned set notices all four attacks with 9 alerts, 7 of them true.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 37 lines, identical on every run |
| Acts | 4 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
