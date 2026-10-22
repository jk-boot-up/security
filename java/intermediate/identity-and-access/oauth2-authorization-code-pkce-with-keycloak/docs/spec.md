# Spec — OAuth 2.0 Authorization Code with PKCE and Keycloak

## Purpose

Teach the OAuth 2.0 authorization code flow with PKCE on a real authorization server and its enforcement of the rules.

## Not in scope

- Operating Keycloak in production.
- OpenID Connect sign-in, which has its own project.
- Device, client-credentials and token exchange grants.

## The problem

Teams copy OAuth settings from examples and leave the implicit flow, wildcard redirects, optional PKCE or reusable refresh tokens switched on.

## The pieces

| Role | Class |
| --- | --- |
| Authorization server | `KeycloakLab`, `infra/bank-realm.json` |
| The app and the browser | `OAuthClient` |
| Resource server | `ApiSecurity`, `AccountsController`, `Running` |
| Time | `OffsetClock` |
| Entry point | `OAuthKeycloakDemo` |

## Requirements

1. The flow gives an access token for the accounts API only through a code redeemed with the right verifier.
2. A failed redemption burns the code.
3. Only the exact registered redirect address gets a code.
4. The app checks the state before it uses a code.
5. A reused code and a reused refresh token end the session.
6. The implicit flow and the password grant are refused.
7. A confidential client needs its secret and PKCE.
8. The API checks signature, issuer, audience, dates and scope.
9. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 33, all passing |
| Demo output | 60 lines, identical on every run |
| Acts | 5 |
| Video | 07:47, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
