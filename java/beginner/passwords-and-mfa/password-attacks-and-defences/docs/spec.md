# Spec — Password Attacks and Defences

## Purpose

Teach how online password guessing works and how defences answer it, by running three attacks against a bank's login with each defence in turn, on a simulated clock, and counting the results.

## Not in scope

- Real network traffic, botnets or CAPTCHAs.
- How one-time codes are generated: the totp project covers that.
- Behavioural signals such as device recognition or unusual locations.

## The problem

Learners know passwords should be strong but not how they are attacked at the login page, or why one defence is never enough.

## The pieces

| Role | Class |
| --- | --- |
| The login with its defences | `Bank` and `Defences` |
| The attacks | `Attacks` |
| Replies and outcomes | `Reply`, `Outcome` |
| Lock settings | `LockScope` |
| The clock | `Ticker` |
| Entry point | `AttacksDemo` |

## Requirements

1. The demo prints five numbered acts and the output equals `docs/expected-output.txt`.
2. A refused attempt (locked or rate limited) never uses up one of the attacker's guesses.
3. A lockout counts failures per account or per account and address, as configured.
4. A successful login resets the count of failures.
5. A right password for an account with a second factor is never an OK.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 21, all passing |
| Demo output | 22 lines, identical on every run |
| Acts | 5 |
| Video | 09:17, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
