# Spec — Secrets in Logs with Logback

## Purpose

Teach keeping secrets out of Logback logs with types, masking and structured events.

## Not in scope

- Shipping or searching logs.
- Log retention tooling.
- Audit logging requirements.

## The problem

Logs are written freely, read widely and kept for long, so what is written in them is exposed.

## The pieces

| Role | Class |
| --- | --- |
| Logging setup | `LogSetup` |
| Masking | `Masker`, `MaskConverter`, `MaskedThrowableConverter` |
| Unprintable types | `Secret`, `Card`, `CheckoutRequest` |
| Entry point | `LoggingDemo` |

## Requirements

1. A masking layout covers the shapes of secrets in messages and exception chains.
2. A card is covered only if it passes Luhn.
3. A line break in a value never makes a second event.
4. Types print safely under a plain layout.
5. JSON masking covers paths and shapes.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 39 lines, identical on every run |
| Acts | 5 |
| Video | 05:40, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
