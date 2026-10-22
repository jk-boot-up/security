# Spec — Session Management with Spring Session and Redis

## Purpose

Teach session management with Spring Session and Redis: what the store changes and what the rules still need.

## Not in scope

- Redis clustering, persistence and failover.
- Token-based login, which has its own projects.
- Single sign-on across applications.

## The problem

Teams move sessions into Redis for scale and leave the session rules, and Redis itself, at their defaults.

## The pieces

| Role | Class |
| --- | --- |
| The bank | `BankController`, `BankSecurity` |
| The absolute limit | `AbsoluteTimeoutFilter`, `OffsetClock` |
| The store | `RedisLab` |
| The device stand-in | `Browser`, `Running` |
| Entry point | `SessionRedisDemo` |

## Requirements

1. Sessions are stored in a Redis that needs a password.
2. The session cookie has every protection.
3. A new id is issued at sign-in.
4. An idle limit and an absolute limit end a session.
5. Sign-out, sign-out everywhere and a password change end sessions on the server.
6. A limit on concurrent sessions ends the oldest.
7. The bank refuses when Redis does not answer.
8. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 15, all passing |
| Demo output | 38 lines, identical on every run |
| Acts | 5 |
| Video | 06:49, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
