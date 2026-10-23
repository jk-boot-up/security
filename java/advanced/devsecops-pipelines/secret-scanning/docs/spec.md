# Spec — Secret Scanning

## Purpose

Teach secret scanning by planting secrets in a repository and measuring scanners and hooks against what was planted.

## Not in scope

- Secret managers and rotation (see kubernetes-secrets).
- Rewriting history with git filter-repo.
- Scanning container images.

## The problem

Secrets are committed by accident, removed by a later commit and forgotten, while scanners are trusted without being measured.

## The pieces

| Role | Class |
| --- | --- |
| The planted truth | `Planted` |
| The repository | `BankRepo` |
| The scanner written here | `Detector` |
| gitleaks | `Gitleaks` |
| The scoring | `Score` |
| Entry point | `SecretScanningDemo` |

## Requirements

1. The repository holds known real secrets and known look-alikes.
2. Scanners are scored against the truth on files and on history.
3. An allowlist and a baseline change the result as the demo prints.
4. A client hook can be skipped and a server hook cannot.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 26 lines, identical on every run |
| Acts | 5 |
| Video | 05:28, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
