# Spec — Kubernetes Admission Control

## Purpose

Teach admission control by enforcing a bank's workload rules with Kyverno on a real cluster, and by showing what it cannot do.

## Not in scope

- Production hardening of Kyverno (replicas, resources, upgrades).
- Gatekeeper and the Pod Security Admission labels.
- Network policies and runtime security (the next projects).

## The problem

Rules for workloads that live in documents are not enforced, and rules enforced only in the pipeline are bypassed by anyone with kubectl.

## The pieces

| Role | Class |
| --- | --- |
| The cluster | `Cluster` |
| The admission controller | `Kyverno` |
| The registry and signatures | `Registry` |
| The requests | `Pods` |
| The answer | `Verdict` |
| Entry point | `KubernetesAdmissionControlDemo` |

## Requirements

1. Policies refuse in labelled namespaces and name the policy that refused.
2. A new rule can start in audit mode and be switched to deny.
3. Images from the bank's registry must carry its cosign signature and are written by digest.
4. An exception works only from the kyverno namespace and only until its date.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 33 lines, identical on every run |
| Acts | 5 |
| Video | 05:57, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
