# Spec — Authentication vs Authorization with Keycloak

## Purpose

Teach how an online store delegates login to Keycloak by building a Spring Boot resource server that validates tokens, maps roles, and answers 401 and 403, and by showing the stale-role and revocation trade-offs.

## Not in scope

- Running a login server in production: clustering, databases and hostname setup.
- Writing an OAuth 2 authorization server: the Spring Authorization Server is a different project.
- Browser flows, cookies and logout.

## The problem

Learners know passwords and sessions but not what changes when login moves to a separate server that hands out tokens.

## The pieces

| Role | Class |
| --- | --- |
| The order API | `Shop` |
| A stand-in login server | `Issuer` |
| Token checks | `Validators`, `Revocations` |
| A simulated clock | `SimClock` |
| The API on a free port | `Running` |
| Entry point | `KeycloakDemo` |
| The real Keycloak (tests) | `Keycloak`, `infra/shop-realm.json` |

## Requirements

1. No token, a forged token, a tampered token, an unsigned token and an expired token all get 401.
2. A valid token without the right role gets 403, and orders are visible to their owner and to staff only.
3. Tokens with the wrong issuer or audience get 401.
4. A role taken away does not affect a token already issued; a short lifetime bounds the stale window.
5. A revoked token is refused and every request costs a lookup.
6. The realm file defines a PKCE public client, a service account client, three roles, an audience mapper and short tokens.
7. The demo prints the same output on every run although the keys are random.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 28, all passing |
| Demo output | 28 lines, identical on every run |
| Acts | 5 |
| Video | 07:43, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
