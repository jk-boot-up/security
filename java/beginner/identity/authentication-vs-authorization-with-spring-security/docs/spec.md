# Spec — Authentication vs Authorization with Spring Security

## Purpose

Teach the difference between authentication and authorization with a framework, by building the same online store as the plain project with Spring Security and comparing the results.

## Not in scope

- A running web server, HTTPS, cookies or CSRF protection: requests go through MockMvc.
- A real user store or single sign-on.
- Spring Boot auto-configuration: the contexts are written out so every piece is visible.

## The problem

Learners meet Spring Security as a block of configuration that appears to protect things. The project shows exactly which lines do the authentication and which do the authorization.

## The pieces

| Role | Class |
| --- | --- |
| Spring Security filter chain | the `SecurityFilterChain` in each config class |
| Accounts and passwords | `CommonConfig` with `CountingPasswordEncoder` |
| The rules | `ShopController` with `@PreAuthorize` |
| Four configurations | `OpenConfig`, `LoginOnlyConfig`, `PolicyConfig`, `LayeredConfig` |
| Running a copy of the shop | `Shop` |
| Entry point | `SpringAuthDemo` |

## Requirements

1. The demo prints five numbered acts and the output equals `docs/expected-output.txt`.
2. A request with no or wrong credentials is answered 401 whenever authentication is required.
3. A known user who is not allowed is answered 403 and the method body never runs.
4. Stored passwords are BCrypt hashes and never the passwords.
5. The export endpoint is open without a rule and closed with the second layer.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 25 lines, identical on every run |
| Acts | 5 |
| Video | 09:20, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
