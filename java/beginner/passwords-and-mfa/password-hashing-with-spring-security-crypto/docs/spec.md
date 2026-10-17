# Spec — Password Hashing with Spring Security Crypto

## Purpose

Teach how a library stores passwords safely by repeating the plain password-hashing story with Spring Security's password encoders, and by showing a migration and the costs of the choices.

## Not in scope

- Spring Security's login filters and sessions: the identity module covers them.
- Benchmarking: costs are shown from the stored text and from arithmetic, not from timing.
- Choosing parameters for a real system.

## The problem

Learners know passwords should be hashed with a library but not what the library does for them, or how the list of accepted algorithms becomes a risk.

## The pieces

| Role | Class |
| --- | --- |
| The encoders | Spring's `PasswordEncoder` implementations |
| The attacker | `Attacker` |
| The customers and wordlist | `Customers` |
| Entry point and helpers | `SpringHashingDemo` |

## Requirements

1. The demo prints six numbered acts and the output equals `docs/expected-output.txt`.
2. A stored text made by any of the four algorithms is checked by its own encoder.
3. An old `{bcrypt}` text still matches after the default changes, and is flagged for upgrade.
4. An allow-list encoder refuses an algorithm that is not on its list.
5. Nothing printed depends on a random salt or on timing.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 15, all passing |
| Demo output | 31 lines, identical on every run |
| Acts | 6 |
| Video | 10:34, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
