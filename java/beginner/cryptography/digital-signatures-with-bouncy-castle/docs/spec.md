# Spec — Digital Signatures with Bouncy Castle and OpenSSL

## Purpose

Teach standard signature formats by signing an online bank's statements with Bouncy Castle, reproducing published test vectors, and having OpenSSL check the results.

## Not in scope

- Certificate chains and revocation: the transport-security module.
- Timestamp authorities: described, not run.
- Hardware security modules.

## The problem

Learners can sign with the JDK but not in a format that anyone else can check, and may not know the one implementation detail that can destroy ECDSA.

## The pieces

| Role | Class |
| --- | --- |
| CMS SignedData | `Cms` |
| ECDSA with both nonce styles | `Ecdsa` |
| Ed25519 and Ed448 | `EdDsa` |
| JWS tokens | `Jws` |
| PEM armor | `PemSignatures` |
| Certificates | `Certificates` |
| The OpenSSL check | `OpenSsl` |
| Entry point | `SignaturesBcDemo` |

## Requirements

1. A CMS message verifies attached and detached, and a change, another certificate or garbage is refused.
2. Ed25519, Ed448, ECDSA (RFC 6979) and JWS match their published values.
3. A reused ECDSA nonce reveals the key in the demonstration.
4. A JWS verifier refuses alg none, a changed payload and a different key.
5. OpenSSL verifies the CMS, Ed25519 and ECDSA signatures, and Java verifies an ECDSA signature OpenSSL made.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 25, all passing |
| Demo output | 33 lines, identical on every run |
| Acts | 5 |
| Video | 07:34, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
