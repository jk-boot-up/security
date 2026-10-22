# Spec — Security Misconfiguration with Spring Boot Actuator

## Purpose

Teach the security settings of Spring Boot Actuator: exposure, values, access, port and login rules.

## Not in scope

- Monitoring, metrics dashboards or alerting.
- Securing the store's own business routes beyond what the demo needs.
- Kubernetes probes and service meshes.

## The problem

Teams copy a tutorial's actuator settings into production, and a stranger can read secrets, map the routes, download memory and stop the store.

## The pieces

| Role | Class |
| --- | --- |
| The store | `StoreApp`, `ShopController` |
| Security rules | `StoreSecurity` |
| The checker | `ActuatorChecker` |
| Running stores | `Running`, `Wire` |
| Entry point | `ActuatorSpringDemo` |

## Requirements

1. With the tutorial setting, a stranger reads the payment key from the environment and from a heap dump.
2. With no settings, only health is offered and it shows a status.
3. With include=* alone, values are masked and heapdump and shutdown stay off.
4. The hardened store uses a separate port, a short list, role rules and read-only loggers.
5. A customer login is not an operator login.
6. The checker changes nothing in the store it checks.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 49 lines, identical on every run |
| Acts | 5 |
| Video | 06:58, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
