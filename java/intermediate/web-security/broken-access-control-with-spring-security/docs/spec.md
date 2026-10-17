# Spec — Broken Access Control with Spring Security

## Purpose

Teach broken access control in Spring by attacking a real Spring Boot store with three common configuration mistakes and fixing it with deny by default, ownership in the query, method security, a denial log and a route inventory test.

## Not in scope

- Authentication methods: the beginner identity module and the intermediate identity module.
- Cross-site request forgery: its own project (CSRF is off here because the API is stateless with credentials in a header).
- Spring Data and a real database: the orders are in memory, looked up the same way.

## The problem

Most access-control bugs in Spring applications are configuration mistakes that the framework enforces faithfully, and they pass every test that does not try another user's data.

## The pieces

| Role | Class |
| --- | --- |
| The rule list | `AccessRules` |
| The fixed filter chain | `GuardedSecurity` |
| The mistaken filter chain | `NaiveSecurity` |
| Who may see which order | `OrderAccess` |
| Orders, by id and by owner | `Orders` |
| Refunds with method security | `RefundService` |
| The denial log | `DenialLog` |
| The route inventory | `RouteInventory` |
| Entry point | `AccessControlSpringDemo` |

## Requirements

1. The naive store lets exactly seven of the nine attacks through.
2. The guarded store refuses all nine attacks and serves all six honest requests.
3. Someone else's order answers 404, signed out answers 401, the wrong role and any route without a rule 403.
4. The refund service refuses a caller without the role even with no URL in between.
5. Every refusal is logged, and no log line contains a password or an Authorization header.
6. The route inventory reports a route without a rule, read from Spring MVC's handler mapping.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 30, all passing |
| Demo output | 45 lines, identical on every run |
| Acts | 5 |
| Video | 08:21, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
