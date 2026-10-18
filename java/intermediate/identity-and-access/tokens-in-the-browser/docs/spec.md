# Spec — Tokens in the Browser

## Purpose

Teach where a single-page app's OAuth tokens should live by stealing them from browser storage, moving them behind a backend for the frontend, and binding
them to a client key with DPoP, all with the JDK and a model browser.

## Not in scope

- The OAuth flow itself: the oauth2-authorization-code-pkce project.
- Real JavaScript, WebCrypto or a real browser.
- Cross-site scripting itself: the cross-site-scripting project.

## The problem

Single-page apps often keep OAuth tokens in localStorage because tutorials do; learners rarely see that every script on the page can read them, or what the alternatives cost.

## The pieces

| Role | Class |
| --- | --- |
| The authorization server | `AuthServer` |
| The bank's API | `BankApi` |
| The backend for the frontend | `Bff` |
| A model browser | `Browser`, `Cookie` |
| A script on the page | `PageScript` |
| DPoP keys and proofs | `DpopKey`, `DpopVerifier` |
| Compact JWS and JSON | `Jws`, `Json`, `Base64Url`, `Ed25519` |
| Entry point | `BffDemo` |

## Requirements

1. A script on the page takes tokens kept in localStorage, and a stolen refresh token works later.
2. With the BFF, the browser holds no token and only a __Host- cookie that scripts cannot read.
3. The BFF refuses state-changing requests without its request header, and the browser refuses a cross-site preflight for it.
4. A DPoP-bound token is refused without a fresh, matching proof from the bound key, and each proof id is accepted once.
5. The JWK thumbprint matches RFC 8037 appendix A.3.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 22, all passing |
| Demo output | 40 lines, identical on every run |
| Acts | 5 |
| Video | 09:08, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
