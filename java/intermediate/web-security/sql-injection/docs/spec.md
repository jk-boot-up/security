# Spec — SQL Injection and Its Defences

## Purpose

Teach SQL injection by attacking a pasted product search with a quote, a comment and a union, then fixing it with parameters, an allow-list and a least-privilege database user.

## Not in scope

- A real database or driver: the engine is a model of the rules that matter here.
- Object-relational mappers and their query languages: the sql-injection-with-hibernate-and-postgres project.
- Blind and time-based injection, which need a real server to measure.

## The problem

Developers join text to build statements, and so a customer can change what the statement means.

## The pieces

| Role | Class |
| --- | --- |
| The SQL engine | `MiniDb`, `SqlException` |
| The shop's data and database users | `Shop` |
| The vulnerable search | `UnsafeSearch` |
| The fixed search | `SafeSearch`, `SortColumn` |
| Entry point | `SqlInjectionDemo` |

## Requirements

1. The pasted search can be made to show an unlisted product and the users table.
2. The fixed search returns nothing for the same inputs, and honest searches, including a quote in a name, still work.
3. A sort column is chosen from a list and anything else falls back to the default.
4. A database user without rights on a table cannot read it even through a vulnerable query.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 16 lines, identical on every run |
| Acts | 4 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
