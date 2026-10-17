# Spec — One-Time Codes (TOTP)

## Purpose

Teach how time-based one-time codes work by building HOTP and TOTP from the standard library, checking them against the standards, and examining the bank's choices and the limits of the method.

## Not in scope

- QR codes and the `otpauth://` format: only Base32 is shown.
- Hardware security keys and WebAuthn: they are mentioned as the answer to phishing.
- Storing secrets encrypted: the secrets are held in memory.

## The problem

Learners use authenticator apps daily but do not know how a code can be the same on two devices that never talk, or where the method stops.

## The pieces

| Role | Class |
| --- | --- |
| The code | `Hotp`, `Totp`, `Base32` |
| The phone | `Authenticator` |
| The bank | `MfaServer` and `Verdict` |
| Where secrets come from | `SecretSource`, `SecureSecrets`, `DemoSecrets` |
| Entry point | `TotpDemo` |

## Requirements

1. HOTP matches the ten values of RFC 4226, and TOTP matches the published values of RFC 6238 for SHA-1, SHA-256 and SHA-512.
2. Base32 matches the vectors of RFC 4648.
3. A code from outside the window is refused, and one inside it is accepted.
4. With replay protection, a code from a step already used is refused.
5. A recovery code works once.
6. The demo output equals `docs/expected-output.txt` on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 52, all passing |
| Demo output | 24 lines, identical on every run |
| Acts | 5 |
| Video | 10:01, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
