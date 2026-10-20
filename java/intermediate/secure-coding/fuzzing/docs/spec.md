# Spec — Fuzzing a Parser

## Purpose

Teach fuzzing by writing a small mutation and coverage-guided fuzzer, finding five planted bugs in an order file parser, shrinking each finding, and fixing the parser with regression tests.

## Not in scope

- Automatic instrumentation or a production fuzzer: the fuzzing-with-jazzer project.
- Fuzzing stateful network services.
- Proving the absence of bugs.

## The problem

Parsers are tested with the inputs their authors expected, and attacked with the inputs they did not.

## The pieces

| Role | Class |
| --- | --- |
| The parsers and their data | `OrderParser`, `BuggyOrderParser`, `FixedOrderParser`, `OrderLine`, `ParseException` |
| Instrumentation | `Coverage`, `Hang` |
| The fuzzer | `Fuzzer`, `Strategy`, `Crash`, `Regression` |
| Entry point | `FuzzDemo` |

## Requirements

1. The only allowed failure is ParseException; anything else, a negative total or a hang is a finding.
2. Random text covers almost nothing; mutation and coverage feedback find the planted bugs; each kind of failure is filed once.
3. A minimized input still fails the same way and cannot lose any single character and keep failing.
4. A finding is turned into the source of a regression test.
5. The fixed parser refuses every minimized input cleanly and survives the same fuzzing.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 29 lines, identical on every run |
| Acts | 4 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
