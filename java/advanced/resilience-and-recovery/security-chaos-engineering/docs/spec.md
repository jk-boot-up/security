# Spec — Security Chaos Engineering

## Purpose

Teach security chaos engineering on a small bank service: fail closed or open, silent loss, unscreened payments, soft-fail revocation and alerts, each measured under injected failure.

## Not in scope

- Load testing.
- Chaos in production.
- A full monitoring stack.

## The problem

Security controls are tested when they work and rarely when their dependencies fail.

## The pieces

| Role | Class |
| --- | --- |
| The payments service | `BankPayments` |
| The five dependencies | `Backends` |
| Toxiproxy from Java | `Toxiproxy`, `Fault` |
| The audit trails | `DroppingAudit`, `SpooledAudit` |
| The monitor | `Monitor` |
| The experiments and scoreboard | `Experiment`, `Lab` |

## Requirements

1. Every dependency is reached through its own Toxiproxy proxy.
2. Each experiment checks a steady state, injects one failure, observes and gives a verdict.
3. The first version has planted weaknesses and fails several experiments.
4. The hardened version holds every hypothesis.
5. Alert delay is counted in failed probes.
6. The demo prints counts and verdicts only, the same on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 167 lines, identical on every run |
| Acts | 5 |
| Video | 06:31, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
