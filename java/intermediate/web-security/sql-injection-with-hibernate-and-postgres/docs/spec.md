# Spec — SQL Injection with Hibernate and PostgreSQL

## Purpose

Teach SQL injection through Hibernate 7 and PostgreSQL: concatenated JPQL and native queries, the fixes, and a database user with few rights.

## Not in scope

- Spring Data repositories (named in the exercises).
- Blind and time-based extraction techniques.
- Database hardening beyond a read-only user.

## The problem

Teams trust the ORM and build queries as strings, and run the application as a database user that can read everything.

## The pieces

| Role | Class |
| --- | --- |
| Entities and data | `Product`, `Account`, `Shop` |
| Vulnerable searches | `UnsafeSearch` |
| Fixed searches | `SafeSearch`, `SortColumn` |
| The database | `PostgresLab` |
| Entry point | `SqlHibernateDemo` |

## Requirements

1. Concatenated JPQL reveals an unlisted product and answers questions about another entity.
2. Concatenated native SQL returns accounts through a union.
3. Named parameters and the Criteria API return nothing for every payload.
4. A sort key outside the list gets the default order.
5. The search user is refused by PostgreSQL when it reads the account table.
6. The container is removed after the demo and after every test run.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 20 lines, identical on every run |
| Acts | 4 |
| Video | 06:01, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
