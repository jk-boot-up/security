# Spec — Privacy Engineering

## Purpose

Teach privacy engineering by measuring minimization, re-identification, retention and erasure on a PostgreSQL database.

## Not in scope

- Legal advice on data protection law.
- Differential privacy.
- Consent management platforms.

## The problem

Privacy is promised in a policy while the data is collected without a purpose, shared with identifying columns, copied and never erased.

## The pieces

| Role | Class |
| --- | --- |
| The database and its seed | `Seed`, `Pg` |
| The purposes | `Purposes` |
| Picking people out | `Anonymity` |
| The pseudonyms | `Pseudonyms` |
| Retention | `Retention` |
| A customer's request | `Subject` |

## Requirements

1. Every personal column is compared with the purposes that need it.
2. The share of people alone in their group is measured on full and on coarse values.
3. Pseudonyms are keyed and stable under one key.
4. A retention job respects a legal hold.
5. The data map is found from the schema and erasure is followed by a scan of every text column.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 29 lines, identical on every run |
| Acts | 5 |
| Video | 05:57, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
