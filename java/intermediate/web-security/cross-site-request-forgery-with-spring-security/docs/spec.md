# Spec — Cross-Site Request Forgery with Spring Security

## Purpose

Teach CSRF defences with Spring Security 7: the default token, the single-page cookie mode, and the ways the protection is lost.

## Not in scope

- A real browser or a front-end framework.
- OAuth and bearer-token flows, which have their own project.
- Cross-site scripting, which is a separate project.

## The problem

Teams disable or exempt Spring Security's CSRF protection to fix a 403, and do not notice what they gave up.

## The pieces

| Role | Class |
| --- | --- |
| The bank | `Bank`, `BankController` |
| Security configuration | `WebSecurity`, `SpaCsrfTokenRequestHandler` |
| The browser stand-in | `Browser` |
| Entry point | `CsrfSpringDemo` |

## Requirements

1. With CSRF off, a forged post from another site moves money.
2. With the default, a forged post and a post with another session's token are refused.
3. The sign-in form, logout and every state-changing route need a token.
4. Single-page mode accepts the cookie value in a header and refuses a post without it.
5. An exempt route checks its own signature, and a GET is never checked for a token.
6. The API chain ignores cookies and needs header credentials.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 35 lines, identical on every run |
| Acts | 5 |
| Video | 06:25, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
