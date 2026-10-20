# Spec — Tokens in the Browser: BFF and DPoP

## Purpose

Teach where a single-page app should keep its tokens by stealing one from local storage, then comparing a backend for the frontend with a DPoP-bound token.

## Not in scope

- A real browser, WebCrypto or a real authorization server.
- Refresh tokens and token rotation: oauth2-authorization-code-pkce.
- The Spring BFF version: the tokens-in-the-browser-with-spring-boot project.

## The problem

Developers keep bearer tokens where every script can read them, and an attacker needs only one script.

## The pieces

| Role | Class |
| --- | --- |
| The page | `BrowserPage` |
| The BFF | `Bff` |
| Tokens and API | `AuthServer`, `ResourceApi`, `ApiReply` |
| DPoP | `DpopProof`, `Jwk`, `MiniJson`, `B64`, `SimClock` |
| Entry point | `TokensDemo` |

## Requirements

1. An injected script reads a token in web storage and non-HttpOnly cookies, and not HttpOnly cookies.
2. A stolen bearer token works from another computer.
3. The BFF refuses requests without its cookie, and state-changing requests without the app's origin and custom header, and ends sessions on sign-out.
4. A DPoP-bound token is refused as a bearer token and without a proof, and every proof check (signature, key, method, address, time, token hash, replay) has a test.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 24 lines, identical on every run |
| Acts | 4 |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output and `animation.html`; `concept.toml`.
