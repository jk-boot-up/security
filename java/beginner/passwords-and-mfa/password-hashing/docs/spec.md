# Spec — Password Hashing

## Purpose

Teach how passwords are stored safely by storing the same five passwords four ways, attacking each with the same list, and counting the attacker's work.

## Not in scope

- Choosing the best algorithm for a real system: bcrypt and Argon2 appear in the with-spring-security-crypto project.
- Password policy, breached-password checks or password managers.
- Real attackers' tools: the attacker is a small loop.

## The problem

Learners know passwords should be hashed but not why a plain hash is not enough. The project makes each improvement visible as a number.

## The pieces

| Role | Class |
| --- | --- |
| The four schemes | `PlainText`, `UnsaltedSha256`, `SaltedSha256`, `Pbkdf2Hasher` |
| Salts | `SaltSource`, `SecureSalts`, `CountingSalts` |
| The accounts table | `AccountStore` |
| The attacker | `Attacker` and `Cracked` |
| Counting work | `WorkMeter` |
| Entry point | `HashingDemo` |

## Requirements

1. The demo prints five numbered acts and the output equals `docs/expected-output.txt`.
2. No scheme except plain text ever stores the password.
3. Two accounts with the same password have different stored values once a salt is used.
4. A hash is checked with the rounds stored in it, not the current setting.
5. A hash with fewer rounds than the current setting is replaced only after a correct password.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 20, all passing |
| Demo output | 26 lines, identical on every run |
| Acts | 5 |
| Video | 09:54, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
