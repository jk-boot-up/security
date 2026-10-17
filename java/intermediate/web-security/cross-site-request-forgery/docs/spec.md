# Spec — Cross-Site Request Forgery (CSRF) and Its Defences

## Purpose

Teach CSRF and its defences by forging a transfer at an online bank from another site's page, then stopping it with form tokens, SameSite cookies, Fetch Metadata and Origin checks, and a rule against state-changing GET.

## Not in scope

- Cross-site scripting, which defeats every CSRF defence because it runs on the bank's own page: the cross-site-scripting project.
- A real browser engine: the browser is a model of the rules that matter here.
- The Spring Security implementation: the tools version.

## The problem

Developers trust a valid session cookie as proof of intent, and so a page on any other site can act as the logged-in customer.

## The pieces

| Role | Class |
| --- | --- |
| Origins and sites | `Site` |
| The bank and its defences | `Bank`, `Protection` |
| The real local server | `BankServer` |
| The browser model | `Browser` |
| Entry point | `CsrfDemo` |

## Requirements

1. Without defences, a cross-site form post moves money.
2. Tokens are required, belong to one session, and are compared in constant time.
3. The browser model follows SameSite rules (Lax by default, Strict, None only with Secure) and a legacy browser ignores them.
4. Fetch Metadata and Origin checks refuse cross-site and same-site requests for state changes.
5. GET never moves money unless the bank is deliberately misconfigured.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 21, all passing |
| Demo output | 37 lines, identical on every run |
| Acts | 5 |
| Video | 08:12, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
