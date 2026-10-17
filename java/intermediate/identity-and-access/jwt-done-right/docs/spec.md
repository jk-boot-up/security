# Spec — JWT Done Right

## Purpose

Teach how to validate JWT access tokens correctly by building a strict validator with the JDK, checked against RFC 8037, and showing for each check what a
naive reader lets through.

## Not in scope

- Issuing tokens through OAuth 2.0 flows: the oauth2-authorization-code-pkce project.
- Encrypted tokens (JWE).
- Storing tokens in a browser: the tokens-in-the-browser project.

## The problem

Learners use JWTs through libraries and rarely see which checks the library is making; the common bugs are checks that were never switched on.

## The pieces

| Role | Class |
| --- | --- |
| Strict base64url | `Base64Url` |
| Strict JSON | `Json` |
| Ed25519 keys and signatures | `Ed25519` |
| Compact JWS | `Jws` |
| Keys by id | `KeySet` |
| The API's settings | `Policy` |
| The validator | `Validator` |
| The mistake | `NaiveReader` |
| Server-side refresh tokens | `RefreshTokens` |
| Entry point | `JwtDemo` |

## Requirements

1. The RFC 8037 example token verifies, re-signs to the same signature, and its public key is derived from its private key.
2. Any algorithm other than EdDSA, a missing or wrong type, a critical header and an unknown key id are refused before the claims are read.
3. Expiry and not-before allow exactly the configured skew; issuer, audience, scope, subject and lifetime are checked.
4. Duplicate JSON members, padding and other base64 alphabets are refused.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 38, all passing |
| Demo output | 40 lines, identical on every run |
| Acts | 5 |
| Video | 09:00, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
