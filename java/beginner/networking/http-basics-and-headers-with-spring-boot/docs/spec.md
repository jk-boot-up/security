# Spec — HTTP Basics and Security Headers with Spring Boot

## Purpose

Teach how a real framework applies the HTTP security basics by running two Spring Boot servers, bare and hardened, scoring their real headers, hardening the session cookie, and attacking them with an injected header and an oversized one.

## Not in scope

- HTTPS, certificates and HSTS preloading: the transport-security module.
- CSRF tokens, authentication and CORS.
- Writing a full Content-Security-Policy for a real site.

## The problem

Learners know the headers by name but have never seen a framework's real response, or what changes it.

## The pieces

| Role | Class |
| --- | --- |
| The two stores | `Shop.Bare`, `Shop.Hardened` |
| Endpoints | `ShopController` |
| Header audit | `HeaderAudit` |
| Cookie judge | `Cookie` |
| Talking to the servers | `Wire` |
| Entry point | `HttpSpringDemo` |

## Requirements

1. A bare store scores 0 of 5 and a hardened store scores 4 of 5 over HTTP and 5 of 5 behind a proxy that ends HTTPS.
2. The session cookie has no problems in the hardened store.
3. A line break in a header never produces a second header.
4. A request over the size limit is refused.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 25 lines, identical on every run |
| Acts | 5 |
| Video | 07:00, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
