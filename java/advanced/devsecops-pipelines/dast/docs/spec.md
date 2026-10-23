# Spec — Dynamic Analysis

## Purpose

Teach dynamic analysis by scanning a running application with known flaws and counting what each kind of scan finds.

## Not in scope

- Writing exploits.
- Scanning applications you do not own.
- API scans from an OpenAPI file.

## The problem

Dynamic scans are run without a login and trusted without a truth to compare with.

## The pieces

| Role | Class |
| --- | --- |
| The application under test | `BankApp` |
| The planted truth | `Flaw` |
| The scan | `Zap` |
| One kind of alert | `Alert` |
| Docker and commands | `Sh` |
| Entry point | `DastDemo` |

## Requirements

1. The application has known planted flaws, one behind a login.
2. A passive, an anonymous and a logged-in scan are counted against them.
3. The fixed version raises none of the planted flaws.
4. The scans run with ZAP in Docker from a generated plan.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 19 lines, identical on every run |
| Acts | 5 |
| Video | 05:13, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
