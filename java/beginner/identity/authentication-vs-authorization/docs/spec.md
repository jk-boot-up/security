# Spec — Authentication vs Authorization

## Purpose

Teach the difference between authentication and authorization by building an online store's order endpoints with no checks, with a login only, and with a login and a policy of roles and ownership.

## Not in scope

- A real login page, cookies, or an HTTP server: requests are method calls.
- Production token design: tokens are numbered for repeatability.
- Multi-factor login, single sign-on or token formats: later projects.

## The problem

Learners treat a login as the whole of access control. The project shows exactly what a login does and does not decide.

## The pieces

| Role | Class |
| --- | --- |
| Who is asking | `Authenticator`, `Credentials`, `Session` |
| What may they do | `PolicyGate` and its rules |
| The three versions | `NoGate`, `LoginOnlyGate`, `PolicyGate` |
| The endpoints | `Shop` |
| Where roles live | `UserDirectory` |
| Entry point | `AuthDemo` |

## Requirements

1. The demo prints five numbered acts and the output equals `docs/expected-output.txt`.
2. A request without a live session is answered 401, never 403.
3. A request from a known user who is not allowed is answered 403, never 401.
4. A session older than its lifetime is not recognised.
5. With the role looked up each time, a demotion takes effect on the next request.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 31, all passing |
| Demo output | 28 lines, identical on every run |
| Acts | 5 |
| Video | 09:20, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
