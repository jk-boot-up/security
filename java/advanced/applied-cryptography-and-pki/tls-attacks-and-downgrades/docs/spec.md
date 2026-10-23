# Spec — TLS Attacks and Downgrades

## Purpose

Teach TLS downgrades and weak key exchange by attacking deliberately weak local servers and counting what the tools find against what was planted.

## Not in scope

- Scanning or attacking servers you do not own.
- Breaking a 1024-bit DH group.
- Padding oracle and compression attacks that need a build with those features.

## The problem

Weak TLS settings are found by a scanner grade that does not say what a recording would reveal.

## The pieces

| Role | Class |
| --- | --- |
| The local servers | `Server` |
| The planted truth | `Weakness` |
| Our own probes | `Audit`, `Probe` |
| testssl.sh and its JSON | `TestSsl` |
| The network that breaks handshakes | `Tap` |
| Reading a leaked session | `Decryptor`, `TlsRecords` |
| Entry point | `TlsAttacksDemo` |

## Requirements

1. Three local servers: weak, strong, TLS 1.3 only.
2. Probes and testssl.sh are counted against 6 planted weaknesses.
3. A retrying client, the fallback signal and a minimum version are compared.
4. A recorded session is opened with a leaked key for static RSA only.
5. A TLS 1.3 only server refuses what TLS 1.3 removed.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 58 lines, identical on every run |
| Acts | 5 |
| Video | 06:34, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
