# Spec — OAuth 2.0 Authorization Code Flow with PKCE

## Purpose

Teach the OAuth 2.0 authorization code flow with PKCE as RFC 9700 describes it, by building an authorization server, a resource server and a client with the
JDK, and showing each attack as a request that the correct implementation refuses.

## Not in scope

- OpenID Connect sign-in and ID tokens: the openid-connect project.
- Tokens in single-page apps and the backend-for-frontend pattern: the tokens-in-the-browser project.
- Validating self-contained JWT access tokens: the jwt-done-right project.

## The problem

Learners meet OAuth through libraries and identity providers and rarely see each message; the common failures are loose redirect matching, missing PKCE,
missing state checks, long-lived codes and refresh tokens that never rotate.

## The pieces

| Role | Class |
| --- | --- |
| The authorization server | `AuthorizationServer` |
| What the bank knows about a client | `ClientRegistration` |
| PKCE | `Pkce` |
| The bank's API | `BankApi` |
| The budgeting app | `BudgetApp` |
| The token endpoint as the app sees it | `TokenEndpoint` |
| The same over HTTP | `LoopbackBank` |
| Query strings and forms | `Urls` |
| OAuth errors | `OAuthException` |
| Entry point | `OAuthDemo` |

## Requirements

1. PKCE matches RFC 7636 appendix B, only S256 is accepted, and a code without the right verifier is refused.
2. Only exactly registered redirect URIs get a code; other errors about the client are shown at the bank.
3. A callback is accepted only with the state its browser session was given.
4. Codes are single-use, expire after 60 seconds, and a reuse revokes what the first use issued.
5. Scopes are limited to what the client registered and enforced by the API; refresh tokens rotate and a reuse revokes the family.
6. The implicit flow and the password grant are refused; confidential clients authenticate.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 33, all passing |
| Demo output | 55 lines, identical on every run |
| Acts | 5 |
| Video | 09:15, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
