# Spec — IaC Scanning

## Purpose

Teach infrastructure-as-code scanning by writing tested rules, comparing real scanners and handling exceptions and baselines.

## Not in scope

- Admission control (see policy-as-code).
- Scanning a running cluster.
- Writing rules for the tools' own languages.

## The problem

Scans produce noise, miss things and collect exceptions, and then nobody trusts them.

## The pieces

| Role | Class |
| --- | --- |
| The rules | `Rules`, `Rule`, `Finding` |
| The engine and its views | `Engine` |
| The manifests | `Manifests` |
| The rule tests | `RuleTester` |
| The real tools | `Scanners` |
| Exceptions | `Waiver` |
| The baseline | `Baseline` |
| Entry point | `IacScanningDemo` |

## Requirements

1. Rules are code over parsed manifests and each has a bad and a good sample.
2. Checkov, Trivy and KICS run on the same files and are compared by rule.
3. Waivers need an owner, a reason and an end date and expire.
4. A baseline separates new findings from accepted ones.
5. Every tool is pinned (Checkov by version, Trivy by image with its built-in checks, KICS by digest), so the demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 118 lines, identical on every run |
| Acts | 5 |
| Video | 16:49, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
