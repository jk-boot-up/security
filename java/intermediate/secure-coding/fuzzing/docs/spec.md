# Spec — Fuzzing: Finding the Inputs Nobody Tested

## Purpose

Teach fuzzing by fuzzing an online store's order and cart parsers, blind and guided by coverage, shrinking each crash to its smallest input, fixing the parsers and fuzzing them again.

## Not in scope

- Bytecode instrumentation and a real fuzzing engine: the -with-jazzer version.
- Logic flaws: the race-conditions-and-business-logic project.
- Fuzzing native code.

## The problem

Learners trust their example tests, so the inputs nobody thought of reach production first.

## The pieces

| Role | Class |
| --- | --- |
| Expected refusal | `ParseException` |
| Branch counters | `Coverage` |
| Order lines | `OrderLine` |
| Carts | `CartParser`, `Value` |
| Harnesses | `Harness` |
| Fuzzer | `Fuzzer`, `Crash` |
| Shrinking | `Minimizer` |
| Entry point | `FuzzingDemo` |

## Requirements

1. The same seed finds the same crashes at the same runs.
2. Blind fuzzing finds the shallow order-parser bugs and the nesting crash.
3. Only the coverage-guided fuzzer finds the gift-message bug within the budget.
4. The minimiser shrinks each crash to an input that crashes the same way.
5. The fixed parsers refuse every smallest input and survive the fuzzer.
6. Parsing, printing and parsing again gives an equal cart.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 41, all passing |
| Demo output | 34 lines, identical on every run |
| Acts | 5 |
| Video | 08:20, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
