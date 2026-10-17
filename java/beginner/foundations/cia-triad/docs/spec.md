# Spec — The CIA Triad

## Purpose

Teach the three promises of the CIA triad by breaking each one in a small online store's order system and restoring it with the simplest control that works.

## Not in scope

- Real cryptography: the seal uses a standard library function and a demo key.
- Real networking or a real database: the server and storage are simulated.
- A complete store: only the parts needed to show the triad.

## The problem

Learners meet security as a list of unrelated tools. The triad gives them one way to ask what a control is for.

## The pieces

| Role | Class |
| --- | --- |
| The insecure system | `NaiveStore` |
| The controlled system | `SecureStore` |
| Integrity seal | `Seal` |
| Availability control | `RateLimiter` with `TickServer` |
| Entry point | `CiaDemo` |

## Requirements

1. The demo prints five numbered acts and the output equals `docs/expected-output.txt`.
2. Support never receives the address or the amount through `SecureStore.read`.
3. A stored order that no longer matches its seal is never returned.
4. One client cannot take more than its share of the server in one tick.
5. Every emergency access has a reason and an audit log entry.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 17, all passing |
| Demo output | 23 lines, identical on every run |
| Acts | 5 |
| Video | 10:17, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
