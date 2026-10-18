# Spec — OpenID Connect

## Purpose

Teach OpenID Connect sign-in by building a strict relying party with the JDK: the flow, ID token validation, discovery, identity by issuer and subject,
and sign-out, with each check shown refusing the token it exists to refuse.

## Not in scope

- A certified implementation: the -with-spring-security-and-keycloak version uses one.
- Front-channel logout, session management iframes and dynamic client registration.
- Encrypted ID tokens (JWE) and pairwise subject identifiers.

## The problem

Learners often meet OpenID Connect through a library and a "sign in with" button, and do not see which checks make the sign-in trustworthy.

## The pieces

| Role | Class |
| --- | --- |
| The identity provider | `Provider` |
| Discovery | `Discovery`, `ProviderConfig`, `KeySet` |
| ID token validation | `IdTokenValidator`, `IdToken` |
| The relying party | `Store` |
| Real HTTP | `LoopbackProvider` |
| The web inside the process | `Web` |
| Tokens | `Jws`, `Json`, `Base64Url`, `Ed25519` |
| Entry point | `OidcDemo` |

## Requirements

1. An ID token is accepted only with a published key, the exact issuer, the store in its audience (and azp when several), the time within skew, and this browser's nonce.
2. Metadata must name the configured issuer, and keys come only from the issuer's own site.
3. Accounts are keyed by issuer and subject; unverified email is not stored.
4. Back-channel logout tokens are typed, carry the logout event, have no nonce and end only the named session.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 27, all passing |
| Demo output | 50 lines, identical on every run |
| Acts | 5 |
| Video | 09:41, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
