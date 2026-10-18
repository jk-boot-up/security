# Spec — Session Management: Ids, Rotation, Timeouts and Sign-Out

## Purpose

Teach session management by building an online bank's session manager: random ids stored as hashes, rotation at sign-in and step-up, idle and absolute timeouts,
server-side sign-out with sign-out everywhere, password-change revocation and a session limit, and a safe session cookie.

## Not in scope

- Token-based sessions such as JWTs: the jwt-done-right project.
- A shared session store in Redis: the -with-spring-session-and-redis version.
- Multi-factor authentication: the totp-multi-factor project.

## The problem

Sessions are often treated as plumbing, so ids are kept across sign-in, sessions never expire on the server, and sign-out only clears the browser.

## The pieces

| Role | Class |
| --- | --- |
| Session ids and their hashes | `SessionIds` |
| The session manager | `SessionManager` |
| Where sessions are kept | `SessionStore` |
| Timeouts and limits | `Settings` |
| The session cookie | `Cookies` |
| A browser's cookie jar | `Browser` |
| The real local server and client | `BankServer`, `BankClient` |
| Entry point | `SessionDemo` |

## Requirements

1. Ids are 128 random bits and the store holds only their SHA-256.
2. Sign-in and step-up issue a new id, and the earlier id is refused.
3. Idle and absolute timeouts end sessions at the configured times, and payments need a recent password.
4. Sign-out, sign-out everywhere and a password change end sessions on the server, and a fourth session ends the oldest.
5. The session cookie has no missing attributes, on a real local server.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 25, all passing |
| Demo output | 40 lines, identical on every run |
| Acts | 5 |
| Video | 08:04, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
