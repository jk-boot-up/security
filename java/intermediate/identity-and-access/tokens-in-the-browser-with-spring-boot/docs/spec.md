# Spec — Tokens in the Browser with Spring Boot: a BFF and DPoP

## Purpose

Teach where a single-page app's tokens should live, with a Spring Boot BFF and DPoP on real servers.

## Not in scope

- A real front-end framework.
- Content-Security-Policy in depth.
- Device-bound sessions and passkeys.

## The problem

Teams keep tokens in the browser because it is the easy way to call an API, and give them to every script on the page.

## The pieces

| Role | Class |
| --- | --- |
| The provider | `KeycloakLab`, `infra/bank-realm.json` |
| The accounts API | `ApiApp`, `ApiSecurity`, `AccountsController`, `Ledger` |
| The BFF | `BffApp`, `BffSecurity`, `BffController`, `CsrfCookieFilter` |
| The page | `SpaClient`, `DpopKey`, `Shopper` |
| Entry point | `BffSpringDemo` |

## Requirements

1. A script on a page that holds tokens can take them and use them elsewhere.
2. A BFF gives the browser a __Host- cookie and holds the tokens on the server.
3. The BFF refreshes an access token that has run out.
4. A change needs the XSRF header, and the cookie is kept off requests that another site starts.
5. A DPoP-bound token is refused as a bearer token and without a valid, fresh, unused proof for the request.
6. A refresh token bound to a key needs the key.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 26, all passing |
| Demo output | 42 lines, identical on every run |
| Acts | 5 |
| Video | 07:37, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
