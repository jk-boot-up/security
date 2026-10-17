# Spec — Broken Access Control

## Purpose

Teach broken access control by attacking a small store API five ways and fixing every attack with one central policy, deny by default, object-level checks and a route inventory test.

## Not in scope

- Authentication: the beginner identity module.
- Framework-specific access control: the Spring Security version of this project.
- Attribute-based access control engines and policy languages: the advanced track.

## The problem

Access control is the most common serious weakness in web applications and APIs, and it is usually missing checks rather than clever attacks.

## The pieces

| Role | Class |
| --- | --- |
| The store's business logic, with no checks | `Handlers` |
| The first version, checks in each handler | `NaiveStore` |
| The rules and the choke point | `Policy` |
| The fixed version | `GuardedStore` |
| Routes and matching | `Route`, `RouteTable` |
| Users, tokens and orders | `ShopData` |
| Real HTTP | `HttpFront` |
| Entry point | `AccessControlDemo` |

## Requirements

1. Every attack in the list succeeds or fails exactly as stated against each store.
2. The guarded store refuses all nine attacks and serves all six honest requests.
3. Someone else's order answers 404, signed out answers 401, the wrong role 403.
4. Every refusal is logged, and no log line contains a token.
5. A route with no rule is refused, and the route inventory test reports it.
6. Tokens are compared in constant time.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 36, all passing |
| Demo output | 40 lines, identical on every run |
| Acts | 5 |
| Video | 08:35, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
