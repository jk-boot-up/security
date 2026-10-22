# Spec — Insecure Deserialization and Its Defences

## Purpose

Teach insecure deserialization by running code through a cart cookie and forging a price, then fixing it with an allow-list filter and a data-only, signed cookie.

## Not in scope

- Real gadget chains from third-party libraries.
- Jackson and other data formats with type information: the insecure-deserialization-with-jackson project.
- Session storage on the server.

## The problem

Developers read objects from bytes that the customer can change, and the bytes choose which code runs.

## The pieces

| Role | Class |
| --- | --- |
| The cart | `Cart` |
| The gadget and the attacker | `TempCleaner`, `Attacker` |
| The cookies | `CartCookie`, `SerializedCartCookie`, `SignedTextCartCookie`, `CartRefused` |
| Entry point | `DeserializationDemo` |

## Requirements

1. An unfiltered reader runs the gadget while reading and believes a forged price.
2. A filtered reader refuses classes outside the allow-list before they are built, and limits size and depth.
3. The signed text cookie refuses objects, changed quantities and unsigned text, and takes prices from the server.
4. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 10, all passing |
| Demo output | 17 lines, identical on every run |
| Acts | 4 |
| Video | 05:56, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
