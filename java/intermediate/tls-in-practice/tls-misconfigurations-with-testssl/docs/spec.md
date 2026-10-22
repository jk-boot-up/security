# Spec — TLS Misconfigurations with testssl.sh

## Purpose

Teach scanning TLS servers with testssl.sh and turning the report into a gate.

## Not in scope

- Writing a scanner.
- The vulnerability checks of testssl.
- Scanning servers on the internet.

## The problem

TLS settings are copied and changed without anyone checking how a client sees them.

## The pieces

| Role | Class |
| --- | --- |
| The root | `LocalCa` |
| The servers | `Nginx`, `LegacyServer` |
| The scanner | `TestSsl`, `Report` |
| The rules | `Policy` |
| Entry point | `TlsAuditDemo` |

## Requirements

1. Five servers start on free loopback ports and stop at the end.
2. testssl.sh scans each by name, and one by address.
3. A policy of rules over the report, with named exceptions that carry a reason.
4. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 14, all passing |
| Demo output | 30 lines, identical on every run |
| Acts | 5 |
| Video | 05:42, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
