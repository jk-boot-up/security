# Spec — Digital Signatures (Ed25519)

## Purpose

Teach digital signatures by showing the forgery that a shared HMAC key allows, signing and checking with Ed25519, tying signatures to a purpose, version and expiry, and showing what a signature cannot do.

## Not in scope

- Certificates, authorities and revocation: the next modules.
- Other signature schemes in depth: ECDSA and RSA-PSS are only mentioned.
- Key storage and rotation.

## The problem

Learners know "sign it" but not what that does that a keyed hash does not, what must be inside what is signed, or where the trust comes from.

## The pieces

| Role | Class |
| --- | --- |
| Keys | `Ed25519Keys` |
| Signing and checking | `Signing` |
| The message | `PriceList` and `Signed` |
| The shop | `Publisher` |
| A service that checks | `Checkout` and `Verdict` |
| The shared-key contrast | `HmacPublisher` |
| Entry point | `SignaturesDemo` |

## Requirements

1. Ed25519 signatures match the published values of RFC 8032, and verify.
2. A signature made for one context is refused for another.
3. A changed message, another key, or a wrong-length signature is refused, never an exception.
4. A checkout with a freshness check refuses an old version and an expired list.
5. The demo output equals `docs/expected-output.txt` on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 24, all passing |
| Demo output | 29 lines, identical on every run |
| Acts | 5 |
| Video | 09:06, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
