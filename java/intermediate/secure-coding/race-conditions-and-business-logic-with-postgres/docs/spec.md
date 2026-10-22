# Spec — Race Conditions with PostgreSQL and Hibernate

## Purpose

Teach closing the gap between a check and a write with the tools of a real database.

## Not in scope

- Distributed transactions.
- Sharding or replication.
- Performance tuning of locks.

## The problem

Requests that are correct one at a time are wrong at the same time.

## The pieces

| Role | Class |
| --- | --- |
| The database | `Db` |
| The Hibernate side | `Hib`, `HibAccount` |
| Entry point | `RaceConditionsDemo` |

## Requirements

1. A real PostgreSQL runs in Docker and is removed at the end.
2. The interleavings are scripted so that the output is the same on every run.
3. Each way of closing the gap is shown against the same bug.
4. A waiting session is shown waiting.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 36 lines, identical on every run |
| Acts | 5 |
| Video | 06:03, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
