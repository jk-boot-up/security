# Spec — OpenID Connect with Spring Security and Keycloak

## Purpose

Teach OpenID Connect login with Spring Security 7 on a real provider: the checks, the account key and the logouts.

## Not in scope

- Operating Keycloak in production.
- Authorization of the store's own routes.
- Passkeys, SAML and other sign-in methods.

## The problem

Teams add OpenID Connect login with a few properties and then key their customers by e-mail address and sign out only locally.

## The pieces

| Role | Class |
| --- | --- |
| The provider | `KeycloakLab`, `infra/*-realm.json` |
| The store | `StoreSecurity`, `StoreController`, `StoreAccounts` |
| The browser | `Shopper` |
| The tokens | `Tokens`, `LocalProvider` |
| Entry point | `OidcSpringDemo` |

## Requirements

1. The sign-in uses state, nonce and PKCE kept for the browser.
2. An access token is not accepted as an ID token.
3. A key never published, a wrong issuer or audience, a missing azp, an expired token, no subject and alg none are refused.
4. Discovery refuses a document naming another issuer.
5. Customers are keyed by issuer and subject.
6. Sign-out reaches the provider; the provider's back-channel logout ends one session.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 33, all passing |
| Demo output | 50 lines, identical on every run |
| Acts | 5 |
| Video | 07:17, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
