# Spec — Secrets Management

## Purpose

Teach how secrets leak and how they are managed safely by scanning a sample repository, demonstrating environment and image-layer leaks,
injecting secrets at run time, leasing per-application logins from a model secrets service, and rotating a key with no downtime.

## Not in scope

- A real secrets server: the -with-openbao version.
- Platform and workload identity: the advanced track.
- Encryption at rest of the secrets store itself.

## The problem

Learners put secrets where they are convenient, and the convenient places are the ones that leak.

## The pieces

| Role | Class |
| --- | --- |
| Secret values | `Secret` |
| Secret scanner | `SecretScanner` |
| Image layers | `ImageLayers` |
| Run-time injection | `MountedSecrets` |
| Secrets service and leases | `SecretsService` |
| The database | `Database` |
| Key rotation | `PaymentProvider` |
| Entry point | `SecretsDemo` |

## Requirements

1. The scanner finds named settings and random-looking values, and ignores placeholders.
2. A secret file readable by others is refused; the Secret type never prints its value and can be wiped.
3. Each application gets its own leased login, which ends at its expiry, stops renewing at the maximum, and ends at once when revoked.
4. Both key versions work during a rotation and only the new one after it.
5. The demo prints the same output on every run and never a secret value.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 17, all passing |
| Demo output | 38 lines, identical on every run |
| Acts | 5 |
| Video | 08:18, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
