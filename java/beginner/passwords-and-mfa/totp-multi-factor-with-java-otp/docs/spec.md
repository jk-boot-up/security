# Spec — One-Time Codes with the java-otp Library

## Purpose

Teach what a one-time-code library gives and what it leaves out, by using java-otp for the codes, checking it against the standards, and writing the window, replay memory and provisioning link around it.

## Not in scope

- Storing secrets securely: they are public test keys held in memory.
- QR code images: only the text they carry.
- Testing real authenticator apps.

## The problem

Learners who move from the hand-written version to a library need to know where the library's responsibility ends.

## The pieces

| Role | Class |
| --- | --- |
| The codes | `java-otp`'s `TimeBasedOneTimePasswordGenerator` |
| The bank's check | `OtpService` |
| The provisioning link | `OtpAuthUri` |
| Test keys | `Secrets` |
| Entry point | `OtpLibraryDemo` |

## Requirements

1. The library's codes equal the published values of RFC 4226 and RFC 6238 for SHA-1, SHA-256 and SHA-512.
2. The window accepts steps within it and refuses steps outside it.
3. With replay protection, a second use of a code is refused, and the memory is kept per customer.
4. The otpauth link carries the issuer, account, Base32 secret without padding, and the settings.
5. The demo output equals `docs/expected-output.txt` on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 31, all passing |
| Demo output | 24 lines, identical on every run |
| Acts | 5 |
| Video | 08:38, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
