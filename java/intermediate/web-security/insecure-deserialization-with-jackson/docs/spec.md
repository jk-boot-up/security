# Spec — Insecure Deserialization with Jackson

## Purpose

Teach insecure deserialization with Jackson 3: class names in JSON, what the default validator does, the three ways to fix a polymorphic field, and data-only requests with limits.

## Not in scope

- Real gadget chains.
- Java's built-in serialization, which the plain project covers.
- Signing or encrypting the cookie.

## The problem

Teams let JSON choose the class to build, for convenience, and trust prices and types that come from the client.

## The pieces

| Role | Class |
| --- | --- |
| The cart and notes | `Cart`, `LooseCart`, `TypedCart`, `Note`, `Line` |
| The gadget stand-in | `TempCleaner` |
| Configuration | `Mappers`, `Cookies` |
| The attacker and the shop | `Attacker`, `OrderRequest`, `Catalogue` |
| Entry point | `JacksonDemo` |

## Requirements

1. A note typed by class name builds a class the cookie names.
2. An Object base type is refused by Jackson 3's default validator.
3. Names from a list and a validator that lists classes both refuse the gadget.
4. A request without prices or types refuses unknown properties, and the shop prices the order.
5. Stream limits bound nesting depth, text and numbers.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 28 lines, identical on every run |
| Acts | 5 |
| Video | 06:24, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
