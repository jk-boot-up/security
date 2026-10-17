# Spec — Least Privilege with Kubernetes RBAC

## Purpose

Teach least privilege on Kubernetes by reading real RBAC manifests, answering what each identity may do, counting the blast radius and escalation paths, linting, and checking the answers against a real kind cluster.

## Not in scope

- Admission control, network policies and pod security standards.
- Groups, users, aggregated roles and non-resource URLs.
- Cloud identity mapping.

## The problem

Learners see RBAC as YAML to copy. They need to see what the rules add up to, and what a stolen token could do.

## The pieces

| Role | Class |
| --- | --- |
| Reading manifests | `Manifests` |
| Answering can-i | `Rbac` |
| Finding bad habits | `Linter` |
| Entry point | `LeastPrivilegeK8sDemo` |
| Real cluster check | `KubectlInteropTest` |

## Requirements

1. The evaluator denies by default and matches verbs, resources and resource names.
2. A namespaced binding applies only in its namespace, even to a ClusterRole.
3. Blast radius and escalation paths are computed for each identity.
4. The linter finds the six problems in the shop manifests.
5. A real kind cluster answers like the evaluator for 490 combinations.
6. The demo needs no cluster and prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 17, all passing |
| Demo output | 39 lines, identical on every run |
| Acts | 5 |
| Video | 07:20, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
