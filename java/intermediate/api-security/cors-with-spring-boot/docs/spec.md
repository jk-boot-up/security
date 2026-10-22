# Spec — CORS with Spring Boot

## Purpose

Teach CORS in Spring Boot: the browser's rules, Spring's CorsConfiguration, and where the policy is registered.

## Not in scope

- A real browser or a front-end framework.
- Cross-site request forgery tokens, which have their own projects.
- Authentication design beyond what the demo needs.

## The problem

Teams silence a CORS error by reflecting the origin or allowing every pattern, and give every site on the web the right to read their customers' data.

## The pieces

| Role | Class |
| --- | --- |
| The bank | `BankController`, `Processed` |
| The policy | `CorsSettings` |
| Security and registration | `BankSecurity` |
| The browser stand-in | `Browser`, `FetchResult` |
| Entry point | `CorsSpringDemo` |

## Requirements

1. With no policy a page can send a simple request but cannot read the answer.
2. A wildcard with credentials is refused by Spring.
3. A reflecting pattern, a null entry and a pattern without a dot each give a lookalike access.
4. An exact list lets only the listed origin read, and Spring refuses the rest before the controller.
5. The preflight is answered only if the policy is registered in Spring Security.
6. A request with no Origin header is not affected by the policy.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 53 lines, identical on every run |
| Acts | 5 |
| Video | 06:51, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
