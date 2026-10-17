# Spec — Symmetric Encryption (AES-GCM)

## Purpose

Teach symmetric encryption by encrypting an online bank's statements five ways and showing what ECB leaks, what GCM adds, what a repeated nonce gives away, and what GCM costs.

## Not in scope

- How AES works inside.
- Key storage, key derivation and rotation: the keystore project covers keys.
- Encrypting large files in chunks, or streaming.

## The problem

Learners know "use AES" but not which mode, what a nonce is, or how a small mistake gives away everything.

## The pieces

| Role | Class |
| --- | --- |
| ECB, to show what not to do | `EcbCipher` |
| AES-GCM | `GcmBox` and `Sealed` |
| Nonces | `NonceSource`, `RandomNonces`, `CountingNonces`, `RepeatedNonce` |
| Entry point | `SymmetricDemo` |

## Requirements

1. AES and GCM match published test values.
2. ECB's cipher text has as many distinct blocks as the plain text.
3. A changed cipher text, tag, nonce, associated data or key makes `open` fail.
4. A repeated nonce lets an attacker who knows one message recover another, shown in a test.
5. The demo output equals `docs/expected-output.txt` on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 21, all passing |
| Demo output | 23 lines, identical on every run |
| Acts | 5 |
| Video | 08:57, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
