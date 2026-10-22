# Spec — CORS Done Right

## Purpose

Teach CORS by showing what the browser lets a page send and read, then breaking it with reflected origins, null and suffix matching, and fixing it with an exact allow-list.

## Not in scope

- A real browser: the browser is a model of the rules that matter.
- Spring's CORS configuration: the cors-with-spring-boot project.
- CSRF defences: the cross-site-request-forgery project.

## The problem

Developers fix CORS errors in the quickest way, and the quickest ways let other sites read answers meant for the customer.

## The pieces

| Role | Class |
| --- | --- |
| The messages | `Request`, `Response`, `FetchResult` |
| The bank and its policies | `BankApi`, `CorsPolicy` |
| The browser model | `Browser` |
| Entry point | `CorsDemo` |

## Requirements

1. Without CORS headers, a cross-origin page cannot read answers, yet a simple POST is processed.
2. A non-simple request is stopped by a failing preflight and never reaches the API.
3. Reflecting the origin, allowing null and suffix matching let attacker pages read the account.
4. An exact allow-list lets only listed origins read, answers preflights narrowly and sets Vary: Origin.
5. The browser model refuses a wildcard with credentials.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 36 lines, identical on every run |
| Acts | 5 |
| Video | 06:30, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
