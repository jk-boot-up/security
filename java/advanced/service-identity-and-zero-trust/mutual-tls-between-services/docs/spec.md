# Spec — Mutual TLS Between Services

## Purpose

Teach mutual TLS between services by running a real private CA and a real server, and measuring who gets in, how identity decides, what rotation costs and what a revoked certificate can still do.

## Not in scope

- A service mesh (the next project).
- Browser or end-user certificates.
- Hardware-protected keys.

## The problem

Services trust the network, so any caller on it is trusted, and certificates are issued once and forgotten.

## The pieces

| Role | Class |
| --- | --- |
| The private CA and the step commands | `Ca` |
| The ledger server, its reload and the revocation check | `Ledger` |
| The identity in a certificate | `Identities` |
| Who may call what | `Policy` |
| The TLS settings that can be replaced while running | `Tls` |
| A calling service | `Caller` |
| Entry point | `MutualTlsDemo` |

## Requirements

1. The CA is a real step-ca and the calls are real TLS.
2. The identity comes from the SAN, never from the subject name.
3. A refused handshake never reaches the application code.
4. The server reloads its certificate and trust without a restart and without a failed call.
5. The demo prints the same output on every run and leaves no key behind.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 9, all passing |
| Demo output | 35 lines, identical on every run |
| Acts | 5 |
| Video | 06:29, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
