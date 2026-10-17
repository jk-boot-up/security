# Spec — Cross-Site Scripting with Spring Boot: Thymeleaf Escaping and a Nonce CSP

## Purpose

Teach cross-site scripting defences in Spring Boot: Thymeleaf's escaping for each place, the th:utext trap, a per-request nonce policy from Spring Security, and session cookie flags.

## Not in scope

- Attack payloads or filter evasion: the problem is shown with a harmless bold tag.
- Client-side frameworks and their own raw-HTML features.
- HTML sanitizers for rich text: mentioned as an exercise.

## The problem

Developers trust the template engine without knowing where its escaping stops, and policies with nonces are rarely wired correctly into Spring Security.

## The pieces

| Role | Class |
| --- | --- |
| The store and its policy | `Shop` |
| A nonce for every request | `NonceFilter` |
| The pages | `ReviewController`, `templates/product.html`, `templates/product-unescaped.html` |
| The policy evaluator | `ContentSecurityPolicy` |
| Reading a page as a browser would | `Markup` |
| The th:utext guard | `Templates` |
| Entry point | `XssSpringDemo` |

## Requirements

1. th:text escapes the review, and th:utext does not.
2. Attribute, link and script values survive whole and stay in their place.
3. Every page has a policy with a fresh nonce, matching the page's script, and a script without it is not allowed.
4. The session cookie is HttpOnly and SameSite=Strict.
5. Only the trap template uses th:utext.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 28 lines, identical on every run |
| Acts | 5 |
| Video | 07:26, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
