# Spec — Passkeys with Spring Security

## Purpose

Teach passkey sign-in with Spring Security's WebAuthn support, and the decisions it leaves to the bank.

## Not in scope

- A browser extension or real authenticator hardware.
- Attestation verification of device makers.
- Federated sign-in.

## The problem

Teams add passkeys with a few lines of configuration and keep the defaults, a password and no clone detection.

## The pieces

| Role | Class |
| --- | --- |
| Security | `PasskeySecurity`, `HardenedRelyingParty`, `PasswordSwitchOffFilter` |
| Controller | `PasskeyController` |
| The browser and page | `PasskeyBrowser` |
| The phone | `SoftwareAuthenticator`, `Cbor`, `B64` |
| Entry point | `PasskeysSpringDemo` |

## Requirements

1. Registration stores a public key and a counter, and nothing that can sign.
2. A challenge is used once, and only by the browser it was given to.
3. A look-alike origin, a foreign relying party id and another key are refused.
4. The hardened bank requires user verification.
5. The hardened bank suspends a passkey whose counter does not go up, after the signature is proved.
6. A passkey can be removed.
7. The password is switched off for a customer with two passkeys.
8. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 31, all passing |
| Demo output | 45 lines, identical on every run |
| Acts | 5 |
| Video | 07:11, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
