# Spec — API Security: Mass Assignment, Data Exposure and Resource Limits

## Purpose

Teach three API weaknesses by promoting a customer through extra JSON keys, reading a whole record and all customers, and hammering the API, then fixing each with an explicit model, a cap and a budget.

## Not in scope

- Object-level authorization (ownership): the broken-access-control project.
- A real HTTP server or a shared rate-limit store: the with-Spring-Boot-and-Redis version.
- Authentication.

## The problem

Developers let the shape of the database record decide what an API accepts and returns, and let callers decide how much work a request does.

## The pieces

| Role | Class |
| --- | --- |
| The data | `Account`, `Bank` |
| The API and its answers | `Api`, `ApiResponse`, `NaiveApi`, `SafeApi` |
| Models | `UpdateProfileRequest`, `AccountView`, `DirectoryEntry` |
| Limits | `RateLimiter`, `SimClock` |
| Entry point | `ApiSecurityDemo` |

## Requirements

1. The naive API sets any field, returns every field and any number of rows, and answers any number of requests.
2. The safe API refuses unknown or invalid fields with 400 and changes nothing.
3. The safe API returns only the view fields, with the card masked, and a directory view in lists.
4. The page size is capped, and calls are limited per caller, charging per row, with 429 and Retry-After.
5. Ownership is still checked in both versions.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 10, all passing |
| Demo output | 22 lines, identical on every run |
| Acts | 5 |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output and `animation.html`; `concept.toml`.
