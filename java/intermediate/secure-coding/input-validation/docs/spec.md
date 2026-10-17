# Spec — Input Validation Done Right

## Purpose

Teach input validation by sending an online store's forms values the page never sends, in unusual spellings and at extreme sizes, first to code that trusts them and then to code that validates at the boundary.

## Not in scope

- Output encoding: the cross-site-scripting project.
- Query parameters and database safety: covered as the reason validation is not enough.
- A validation framework: the -with-hibernate-validator version.

## The problem

Learners validate in the page, use block-lists, and compare text as typed, so their checks are skipped, incomplete or fooled by another spelling.

## The pieces

| Role | Class |
| --- | --- |
| Refusal | `Rejected` |
| Value types | `Quantity`, `Sku`, `Email`, `CountryCode`, `Money` |
| The boundary | `Checkout` |
| Names | `Username`, `DisplayName`, `Accounts` |
| Limits | `Limits` |
| Pattern cost model | `Backtracking` |
| Entry point | `ValidationDemo` |

## Requirements

1. The server-side checkout refuses what the page would refuse, unknown fields and products, and prices from the catalogue.
2. Each value type accepts its allowed shape and refuses a list of other values.
3. Every spelling of a login name has one canonical form, independent of the machine's locale.
4. Bodies, carts and fields over their limits are refused, and a large body costs only the limit to refuse.
5. Refusal messages never contain the raw input.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 83, all passing |
| Demo output | 53 lines, identical on every run |
| Acts | 5 |
| Video | 08:13, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
