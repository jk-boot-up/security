# Spec — Kubernetes Secrets

## Purpose

Teach the limits of Kubernetes Secrets by reading them raw from etcd and by replacing a long-lived secret with short-lived tokens.

## Not in scope

- A KMS provider and key rotation in production.
- A production OpenBao (storage, unsealing, audit).
- External Secrets Operator and the CSI driver.

## The problem

Secrets are believed to be encrypted because they are called secrets, and tokens are left to live forever.

## The pieces

| Role | Class |
| --- | --- |
| The cluster and etcd reads | `Cluster` |
| The encryption configuration | `Encryption` |
| A raw etcd value | `Stored` |
| A ServiceAccount token | `Jwt` |
| OpenBao setup and calls | `OpenBao` |
| The manifests | `Manifests` |
| Entry point | `KubernetesSecretsDemo` |

## Requirements

1. A Secret's value is recovered with no key.
2. Before encryption etcd holds the value as plain text.
3. After the configuration only new and rewritten secrets are encrypted.
4. A pod gets a token that is refused after 10 seconds.
5. A bound token stops working with its pod and the old one does not.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 14, all passing |
| Demo output | 38 lines, identical on every run |
| Acts | 5 |
| Video | 06:19, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
