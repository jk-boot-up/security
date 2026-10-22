# Spec — Threat Modeling with pytm

## Purpose

Teach threat modeling with pytm: a model, its answers, its findings and what remains for people.

## Not in scope

- Replacing the register of decisions.
- Writing pytm's threat library.
- Sequence diagrams.

## The problem

Threat modeling by hand is slow and forgets questions; a tool asks them all but needs honest answers.

## The pieces

| Role | Class |
| --- | --- |
| The design | `Design` |
| The answers | `Answers` |
| The model file | `Writer` |
| pytm | `Pytm` |
| Entry point | `PytmDemo` |

## Requirements

1. The Python model is written from the Java design.
2. pytm runs in a virtual environment installed on the first use.
3. Findings are sorted before they are printed.
4. The diagram and the report come from the same model.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 40 lines, identical on every run |
| Acts | 5 |
| Video | 05:41, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
