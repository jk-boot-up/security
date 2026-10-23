# Spec — Controls and Evidence

## Purpose

Teach controls and evidence by mapping a catalogue to two frameworks and deriving each control's status from dated evidence.

## Not in scope

- A full ISO 27001 certification process.
- Legal advice on compliance.
- Tools for evidence collection from cloud accounts.

## The problem

Compliance is shown once a year from memory, while the controls change every day.

## The pieces

| Role | Class |
| --- | --- |
| A control | `Control` |
| A test run or attestation | `Evidence` |
| The status rules | `Assessor` |
| The catalogue | `catalogue/controls.csv` |
| The test results | `evidence/` |
| Entry point | `ControlsAndEvidenceDemo` |

## Requirements

1. Every control has an owner, a CSF subcategory and an Annex A control.
2. Evidence is read from JUnit XML or from a dated attestation.
3. Automated evidence is stale after 90 days and an attestation after 365.
4. A failing test makes a control failing.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 10, all passing |
| Demo output | 24 lines, identical on every run |
| Acts | 5 |
| Video | 05:44, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
