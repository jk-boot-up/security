# Spec — Service Mesh and Automatic mTLS

## Purpose

Teach what a service mesh gives (identity, encryption, authorization) and what it leaves open, by measuring a real Linkerd cluster.

## Not in scope

- Traffic management, retries and canaries.
- Multi-cluster meshes.
- Choosing a mesh product.

## The problem

Teams install a mesh and assume the cluster is safe, without measuring encryption, policy or the gaps.

## The pieces

| Role | Class |
| --- | --- |
| The cluster and Linkerd | `Cluster`, `Linkerd` |
| The bank's workloads | `Service`, `Lab` |
| The policy objects | `Policies` |
| The design and the scoring | `Design` |
| Proxy counters and certificates | `Metrics`, `CertInfo` |
| The packet capture | `Sniffer` |
| Entry point | `ServiceMeshMtlsDemo` |

## Requirements

1. Pod traffic is captured before and after the proxies.
2. Each identity is read from the proxy's own certificate.
3. The policy leaves exactly the designed pairs.
4. Each gap of the mesh is measured, not claimed.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 15, all passing |
| Demo output | 58 lines, identical on every run |
| Acts | 5 |
| Video | 06:40, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
