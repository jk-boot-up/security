# Spec — Fuzzing with Jazzer

## Purpose

Teach fuzzing Java parsers with the real Jazzer: targets, seeds, findings, minimal inputs and regression tests.

## Not in scope

- Writing a fuzzer.
- Fuzzing native code.
- Continuous fuzzing services.

## The problem

Example tests cover the inputs someone thought of; parsers fail on the rest.

## The pieces

| Role | Class |
| --- | --- |
| The parsers | `OrderLine`, `CartParser`, `Money` |
| Fuzz targets | `targets.*`, `Parsers` |
| Running Jazzer | `JazzerRun`, `Finding` |
| Making inputs small | `Minimizer`, `Crashes` |
| Entry point | `JazzerFuzzDemo` |

## Requirements

1. Each parser has a fuzz target of a few lines.
2. Jazzer is started with a fixed random seed and a fixed number of runs.
3. Findings are read from Jazzer's report and are distinct by kind and place.
4. Each crashing input is made smallest and replayed as a regression test.
5. The fixed parsers run clean under the same fuzzer.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 29, all passing |
| Demo output | 31 lines, identical on every run |
| Acts | 5 |
| Video | 05:48, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
