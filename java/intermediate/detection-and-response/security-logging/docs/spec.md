# Spec — Security Logging

## Purpose

Teach security logging by logging one morning at an online bank: what to record, what never to record, how to keep user text from writing the log, how to show a change, and what logging costs.

## Not in scope

- A logging framework and a log server: the -with-loki-and-grafana version.
- Detection rules and alerts: the detecting-attacks project.
- Signing the head hash: an exercise.

## The problem

Learners log for debugging, and only discover during an incident that the log cannot answer the questions, holds secrets, or can be changed.

## The pieces

| Role | Class |
| --- | --- |
| A security event | `SecurityEvent` |
| The security log and its chain | `SecurityLog` |
| What may be written down | `Redactor` |
| Escaped JSON lines | `Json` |
| Checking the chain | `ChainCheck` |
| Retention with an anchor | `Retention` |
| The old logger | `PlainLogger` |
| The bank's morning | `BankDay` |
| Entry point | `SecurityLoggingDemo` |

## Requirements

1. Every line has the same fields and a request id, and the transfer can be traced to its source address.
2. No secret from the bank's events appears in the log, whether in a named field or in free text.
3. Any text round-trips through a log line without breaking it or adding a field.
4. A deleted, edited or garbled line breaks the chain; an edited or removed last line is found by the published head.
5. Pruned logs still check from their anchor.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 15, all passing |
| Demo output | 38 lines, identical on every run |
| Acts | 5 |
| Video | 09:08, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
