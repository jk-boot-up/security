# Spec — Cross-Site Scripting: Output Encoding and CSP

## Purpose

Teach how cross-site scripting arises and how it is prevented, by showing one review change a page and then building contextual encoders, a comparison with input filtering, and a strict, nonce-based policy on a real local page.

## Not in scope

- Attack payloads and filter evasion: the problem is shown with a harmless bold tag.
- HTML sanitizers for rich text.
- Template engines: the tools version.

## The problem

Developers often know XSS by name and fix it by filtering input, which breaks honest text and leaves contexts open.

## The pieces

| Role | Class |
| --- | --- |
| Contextual encoders | `Encode` |
| A model of how a browser reads the page | `Markup` |
| The product page, unsafe and safe | `ReviewPage` |
| The tempting filter | `NaiveFilter` |
| Nonces, the policy and its evaluator | `ContentSecurityPolicy` |
| The real local page | `ShopServer` |
| Entry point | `XssDemo` |

## Requirements

1. Unencoded review text adds an element to the page; encoded text adds none and reads back exactly.
2. Each encoder keeps its value from ending its context, and is the same in every locale.
3. The input filter changes honest text and misses the attribute context.
4. Only a script with the response's nonce is allowed; 'unsafe-inline' is ignored when a nonce is listed.
5. Every real response has a fresh nonce and an HttpOnly session cookie.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 22, all passing |
| Demo output | 32 lines, identical on every run |
| Acts | 5 |
| Video | 08:21, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
