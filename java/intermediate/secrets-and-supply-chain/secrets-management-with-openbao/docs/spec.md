# Spec — Secrets Management with OpenBao

## Purpose

Teach secrets management with a real secrets server: identity, policy, leases, rotation, revocation and failure.

## Not in scope

- Running OpenBao in production.
- Encryption as a service and PKI.
- Cloud secret managers.

## The problem

Teams keep long-lived shared secrets in code and configuration, and have no way to contain a leak.

## The pieces

| Role | Class |
| --- | --- |
| The lab | `BaoLab` |
| The client | `Bao` |
| Entry point | `BaoSecretsDemo` |

## Requirements

1. A secret id works once, and a token carries only its application's policy.
2. Secrets are versioned and the audit trail hashes their values.
3. Each application gets a database login of its own, and the login really connects with only its grants.
4. A renewal is capped at the maximum, an unrenewed lease ends, and a revoked one ends at once.
5. A rotated static password ends the old one.
6. A sealed server refuses every request and recovers when unsealed.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 41 lines, identical on every run |
| Acts | 5 |
| Video | 06:34, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
