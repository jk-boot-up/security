# Spec — Asymmetric Encryption (RSA and Key Agreement)

## Purpose

Teach how two parties who have never met can share a secret, with RSA-OAEP, hybrid encryption and X25519 key agreement, and show the one thing they cannot do: say whose public key it is.

## Not in scope

- How RSA or elliptic curves work inside.
- Certificates, signatures and trust: the next projects.
- Storing private keys safely: the keystore project.

## The problem

Learners know "public key cryptography" as a phrase but not what the keys are for, why data is not locked with RSA directly, or why a swapped public key defeats it all.

## The pieces

| Role | Class |
| --- | --- |
| RSA with OAEP | `RsaBox` |
| Hybrid encryption | `HybridBox` and `Envelope` |
| Key agreement | `KeyAgreementBox` |
| The shared network | `Wire` |
| Entry point | `AsymmetricDemo` |

## Requirements

1. RSA locks at most 190 bytes at 2048 bits with OAEP-SHA-256, and the same message locked twice differs.
2. Another private key, a changed bit or other associated data makes opening fail.
3. X25519 and HKDF match the published test values of RFC 7748 and RFC 5869.
4. Both sides of a key agreement derive the same key, and an eavesdropper's key differs.
5. The demo prints the same output on every run although its keys are random.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 24, all passing |
| Demo output | 30 lines, identical on every run |
| Acts | 5 |
| Video | 09:34, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
