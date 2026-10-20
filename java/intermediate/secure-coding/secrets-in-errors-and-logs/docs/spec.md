# Spec — Secrets in Errors and Logs

## Purpose

Teach what must stay out of error pages and logs by showing a stack trace with a database password, a log with a card number, a forged log line, and then a fixed checkout, a secret wrapper and a redactor.

## Not in scope

- A logging framework or log shipping: the secrets-in-errors-and-logs-with-logback project.
- What logs must contain for detection: the security-logging project.
- PCI DSS compliance as a whole.

## The problem

Developers put detail in places that many people can read, and cannot take it back.

## The pieces

| Role | Class |
| --- | --- |
| Logging | `Logger`, `NaiveLogger`, `SafeLogger`, `LogSink` |
| Redaction | `Redactor`, `Luhn`, `Secret` |
| The shop | `Checkout`, `Database` |
| Entry point | `LeaksDemo` |

## Requirements

1. The naive checkout reveals the database URL and password and the call chain in its response.
2. The naive logger keeps card, CVV, token and password, and lets a name forge a log line.
3. The fixed checkout answers with a reference and logs the detail redacted, on one line.
4. The redactor masks Luhn-valid card numbers, key-named values, bearer tokens, JWTs and URL passwords, and leaves non-card numbers alone.
5. A Secret prints as [REDACTED].
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 25 lines, identical on every run |
| Acts | 4 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
