# Spec — Kubernetes Network Policies

## Purpose

Teach network policy by applying it to a real cluster and measuring the result against a design.

## Not in scope

- Service meshes and mutual TLS (see service-identity-and-zero-trust).
- Calico and Cilium policy extensions.
- Traffic from outside the cluster.

## The problem

Network policies are written from examples and rarely tested, so a wrong one looks the same as a right one.

## The pieces

| Role | Class |
| --- | --- |
| The pods | `Workload` |
| The design and its comparison | `Intent` |
| The manifests | `Policies` |
| The cluster and the probes | `Lab` |
| Kubernetes tools | `Cluster` |
| Entry point | `KubernetesNetworkPoliciesDemo` |

## Requirements

1. With no policy every pair of pods is reachable.
2. Default deny plus the design's allows gives a measurement equal to the design.
3. Each mistake changes the measurement in the way the demo prints.
4. Denying egress stops name lookups until DNS is allowed.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 22 lines, identical on every run |
| Acts | 5 |
| Video | 05:42, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
