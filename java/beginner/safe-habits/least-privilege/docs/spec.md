# Spec — The Principle of Least Privilege

## Purpose

Teach least privilege by modelling an online store's permissions, measuring blast radius, requiring two people for refunds, expiring grants, auditing unused permissions and locking files to their owner.

## Not in scope

- Cloud IAM and Kubernetes RBAC: the tools projects.
- Authentication: the identity module.
- Sandboxing code inside a JVM: the SecurityManager has been removed.

## The problem

Broad permissions are convenient and invisible until an account is stolen; learners need to see the size of the damage as a number.

## The pieces

| Role | Class |
| --- | --- |
| Policy and decisions | `Policy` |
| The store's permissions and roles | `Catalogue` |
| Users, roles and expiring grants | `Access` |
| Two-person refunds | `RefundDesk` |
| Unused permission audit | `UsageAudit` |
| File permissions | `OwnerOnly` |
| Entry point | `LeastPrivilegeDemo` |

## Requirements

1. Nothing is allowed unless a statement allows it, and an explicit deny beats any allow.
2. The blast radius of a credential is the count of catalogue permissions it reaches.
3. No person can create and approve the same refund, whatever their roles.
4. A grant is active from its start tick up to but not including its end tick, and never overrides a deny.
5. A permission unused for more than the limit, or never used, is flagged.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 28, all passing |
| Demo output | 34 lines, identical on every run |
| Acts | 5 |
| Video | 06:42, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
