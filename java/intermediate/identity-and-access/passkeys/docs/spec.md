# Spec — Passkeys and WebAuthn

## Purpose

Teach how passkeys work and why they resist phishing, by building a model relying party, browser and authenticator with the JDK, laid out as WebAuthn
lays them out, and proving each check with a refusal.

## Not in scope

- CBOR, COSE keys and attestation formats.
- A real browser or authenticator.
- Passkeys in a framework: the -with-spring-security version.

## The problem

Learners hear that passkeys are phishing-resistant but rarely see why: which bytes are signed, who writes the origin, and what the site must check.

## The pieces

| Role | Class |
| --- | --- |
| Base64url | `B64` |
| SHA-256 | `Sha256` |
| Client data | `ClientData` |
| Authenticator data | `AuthenticatorData` |
| The device | `Authenticator` |
| The browser's rules | `Browser` |
| The bank | `RelyingParty` |
| Simulated time | `SimClock` |
| Entry point | `PasskeysDemo` |

## Requirements

1. The bank stores only a credential id, a public key and a counter.
2. Every answer is refused for a used or expired challenge, a wrong type or origin, another relying party id, missing flags, an unknown credential, a bad signature or a counter that did not rise.
3. The browser refuses a relying party id that is not the page's own site.
4. Payments require user verification.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 37, all passing |
| Demo output | 38 lines, identical on every run |
| Acts | 5 |
| Video | 08:53, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
