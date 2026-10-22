# Spec — JWT Done Right with Nimbus JOSE + JWT

## Purpose

Teach JWT validation with Nimbus JOSE + JWT: what the library does, what it leaves to the API, and how keys are fetched and rotated.

## Not in scope

- An authorization server.
- Encrypted tokens (JWE).
- Spring Security's resource server, which wraps this library.

## The problem

Teams pick up a JWT library and assume that it validates everything, and leave the algorithm, the type, the key source and the policy to defaults.

## The pieces

| Role | Class |
| --- | --- |
| The bank | `BankIssuer`, `RefreshTokens` |
| The key set | `JwksServer` |
| The API | `AccessTokenValidator`, `Refused` |
| Time | `SimClock` |
| Entry point | `NimbusJwtDemo` |

## Requirements

1. A parsed token is not trusted until its signature is verified.
2. The algorithm, the type, the issuer and the audience are fixed by the API.
3. Expiry allows 30 seconds of clock skew and no more.
4. Keys come from the one configured URL, cached, with a minimum time between refetches.
5. A jku or jwk header is never used.
6. The scope and the lifetime policy are checked after the library.
7. A new key can be published before it is used, and a retired key is refused once the cache expires.
8. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 28, all passing |
| Demo output | 43 lines, identical on every run |
| Acts | 5 |
| Video | 07:16, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
