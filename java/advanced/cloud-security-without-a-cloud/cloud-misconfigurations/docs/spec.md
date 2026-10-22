# Spec — Cloud Misconfigurations

## Purpose

Teach cloud misconfigurations by deploying, scanning, fixing and attacking a bank's setup on an emulator.

## Not in scope

- Real AWS accounts.
- Policy evaluation (see cloud-iam-models).
- Runtime threat detection.

## The problem

Cloud settings are easy to get wrong and hard to see.

## The pieces

| Role | Class |
| --- | --- |
| The account | `Environment` |
| The emulator | `Venv.Moto` |
| The scanner | `Scanner` |
| The file scanners | `IacScanners` |
| The metadata service | `Imds` |
| Entry point | `CloudMisconfigurationsDemo` |

## Requirements

1. The setup is deployed with the AWS SDK to a local emulator.
2. Nine rules scan buckets, policies, groups, servers and disks.
3. The fixes leave no findings.
4. Checkov and Trivy scan the same setup as files and are compared.
5. The metadata service is simulated in both versions.
6. The demo prints the same output on every run and never prints an id.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 14, all passing |
| Demo output | 34 lines, identical on every run |
| Acts | 5 |
| Video | 05:49, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
