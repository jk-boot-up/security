# Spec — Workload Identity

## Purpose

Teach workload identity by measuring a shared secret and SPIRE SVIDs against the same ledger, with a real SPIRE server and agent.

## Not in scope

- Running SPIRE in production (high availability, a stronger node attestor, federation).
- Kubernetes attestation.
- JWT-SVIDs.

## The problem

Services prove who they are with a secret that every copy shares and that never expires.

## The pieces

| Role | Class |
| --- | --- |
| Authorization by SPIFFE ID | `Policy` |
| The shared-secret ledger | `LegacyLedger` |
| SPIRE in Docker | `Lab` |
| The Java workloads | `Workload` |
| Counting rotations | `Rotations` |
| Entry point | `WorkloadIdentityDemo` |

## Requirements

1. A shared secret is measured as the before: 3 holders, a leak, an eviction.
2. SPIFFE IDs come from registration entries, not claims.
3. Two Java services verify each other's SPIFFE ID and the ledger authorizes by it.
4. SVIDs rotate on their own and the rotations are counted.
5. A stolen SVID and a process with no entry are measured.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 10, all passing |
| Demo output | 47 lines, identical on every run |
| Acts | 6 |
| Video | 06:11, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
