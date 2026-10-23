# Spec — Detection as Code

## Purpose

Teach detection engineering by scoring Sigma rules against labelled logs with budgets, tests and a coverage matrix.

## Not in scope

- Impossible travel and machine learning detections.
- Writing a Sigma backend.
- Running a production SIEM.

## The problem

Detections are written and judged by how quiet they are, with no truth to compare with.

## The pieces

| Role | Class |
| --- | --- |
| A rule | `Rule` |
| Sigma evaluation | `Sigma`, `Engine` |
| The recorded logs | `Corpus` |
| The score | `Score` |
| Tests and mutants | `RuleTests`, `Mutator` |
| ATT&CK matrix | `Coverage` |
| pySigma and OpenSearch | `Pysigma`, `OpenSearch` |
| Entry point | `DetectionAsCodeDemo` |

## Requirements

1. Eight Sigma rules with ATT&CK tags.
2. A seeded corpus with benign, look-alike and labelled attack events.
3. Precision, recall and a budget check per rule.
4. Rule tests and mutation of every rule.
5. A coverage matrix with gaps.
6. pySigma queries compared with the Java evaluation in OpenSearch.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 35, all passing |
| Demo output | 82 lines, identical on every run |
| Acts | 6 |
| Video | 05:27, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
