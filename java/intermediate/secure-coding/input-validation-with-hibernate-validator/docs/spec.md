# Spec — Input Validation with Hibernate Validator

## Purpose

Teach writing the rules of a form as Jakarta Validation constraints with Hibernate Validator, and what they leave to other code.

## Not in scope

- A web framework's binding of requests to objects.
- Output encoding.
- Business rules that need a database.

## The problem

Validation rules are scattered, repeated and unordered, so they drift and cost more than they should.

## The pieces

| Role | Class |
| --- | --- |
| The form | `Checkout`, `Line` |
| Custom rules | `NoInvisible`, `Country`, `PostalCodeMatchesCountry`, `InStock` |
| The validator | `Checks` |
| The service | `Orders` |
| Entry point | `BeanValidationDemo` |

## Requirements

1. Constraints are annotations on records and a parameter.
2. All violations are reported at once with their paths.
3. Cheap checks run before expensive ones, by group sequence.
4. A length limit runs before a pattern.
5. A message template built from visitor text is not evaluated by default.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 53 lines, identical on every run |
| Acts | 5 |
| Video | 05:54, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
