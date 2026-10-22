# Spec — Security Logging with Loki and Grafana

## Purpose

Teach shipping, querying and alerting on security logs with a real Loki and Grafana.

## Not in scope

- Running Loki in production.
- Shipping agents.
- Alert routing and paging.

## The problem

Logs on many machines cannot answer a question about the whole system, and nobody is told when an attack begins.

## The pieces

| Role | Class |
| --- | --- |
| The servers | `Lab` |
| The events | `Event` |
| Loki's API | `Loki` |
| Grafana's API | `Grafana` |
| Entry point | `LokiLogsDemo` |

## Requirements

1. Events are pushed as JSON lines with two labels and a tenant.
2. Questions are asked in LogQL and the answers printed as counts and names.
3. A rule fires for failed logins from one address; a rule with a typo is shown not to.
4. Tenants are separated and shown to be.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 29 lines, identical on every run |
| Acts | 5 |
| Video | 05:44, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
