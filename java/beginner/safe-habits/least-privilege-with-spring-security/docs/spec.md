# Spec — Least Privilege with Spring Security

## Purpose

Teach least privilege with Spring Security by starting a real Spring Boot store and checking deny-by-default rules, method security, separation of duties, a role hierarchy with a measured blast radius, and time-limited grants.

## Not in scope

- Authentication methods, tokens and single sign-on.
- Persistence of accounts and grants.
- Kubernetes and cloud access policies.

## The problem

Teams know the principle and still give the export job admin rights, because the framework lets them.

## The pieces

| Role | Class |
| --- | --- |
| Accounts and actions | `Catalogue` |
| The security configuration | `Shop` |
| Refund methods | `Refunds`, `RefundRules` |
| Expiring grants | `Grants`, `GrantAuthorization`, `SimulatedClock` |
| Blast radius | `BlastRadius` |
| Entry point | `LeastPrivilegeSpringDemo` |

## Requirements

1. A path with no rule is denied, even for an admin.
2. A refund can be created and approved only with the right authority, and never approved by its requester.
3. A role hierarchy lets an admin act as support and does not let finance do so.
4. The blast radius of each account is computed by Spring Security's own authorization manager.
5. A grant works only inside its window, for its person and its permission.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 36 lines, identical on every run |
| Acts | 5 |
| Video | 08:01, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
