# Spec — Threats and Risk

## Purpose

Teach the vocabulary of risk and the habit of ranking by building a small risk register for an online store, with a budgeted plan and an honest look at how subjective the scores are.

## Not in scope

- A full threat-modeling method such as STRIDE: that belongs to a later track.
- Money figures for impact or probability estimates: scores are 1 to 5 on purpose.
- Discovering real vulnerabilities: the risks are given.

## The problem

Learners meet security as an endless list of things to fix. A ranked register gives them a way to decide what to fix first and to explain why.

## The pieces

| Role | Class |
| --- | --- |
| The attack surface | `Surface` and `EntryPoint` |
| One scored risk | `Risk` and `Rating` |
| The ranked list | `RiskRegister` |
| Choosing treatments | `Planner`, `Treatment`, `Strategy` and `Plan` |
| Entry point | `ThreatsDemo` |

## Requirements

1. The demo prints five numbered acts and the output equals `docs/expected-output.txt`.
2. A risk score is likelihood times impact, with both scored from 0 to 5.
3. The ranking is identical on every run, with ties broken by impact and then by id.
4. The planner never spends more than the budget and never funds a treatment that removes nothing.
5. Rescoring one register for a reviewer never changes the original.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 17, all passing |
| Demo output | 32 lines, identical on every run |
| Acts | 5 |
| Video | 10:06, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
