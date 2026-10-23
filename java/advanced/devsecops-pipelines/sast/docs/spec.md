# Spec — Static Analysis

## Purpose

Teach static analysis by writing rules, scoring them on planted flaws and safe lines, testing them, and triaging what they report.

## Not in scope

- CodeQL and whole-program analysis.
- Fixing the flaws.
- Language-specific frameworks beyond the rules shown.

## The problem

Static analysis tools report a lot, rules are written without tests, and nobody measures what they miss.

## The pieces

| Role | Class |
| --- | --- |
| The planted truth | `Marker` |
| The scan and the rule tests | `Semgrep` |
| The scoring | `Score` |
| The suppression check | `Suppressions` |
| The tool install | `Venv` |
| Entry point | `SastDemo` |

## Requirements

1. Every flaw and every safe look-alike is marked in the code under test.
2. Pattern and taint rules are scored on the same lines.
3. A rule with a missing sanitizer fails a rule test.
4. A suppression without a rule, a reason and an owner fails the check.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 24 lines, identical on every run |
| Acts | 5 |
| Video | 05:33, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
