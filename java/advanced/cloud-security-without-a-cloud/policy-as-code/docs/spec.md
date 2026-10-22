# Spec — Policy as Code

## Purpose

Teach policy as code by writing, breaking, testing and enforcing a bank's deployment policy in Rego.

## Not in scope

- Running a webhook in a cluster (see kubernetes-admission-control).
- Rego beyond the rules used here.
- Gatekeeper and Kyverno.

## The problem

Deployment rules that live on a wiki are not enforced, and policies that live in code are rarely tested.

## The pieces

| Role | Class |
| --- | --- |
| The policy | `policy/main.rego` |
| The tools | `Opa` |
| The requests | `Cases` |
| The tests of the tests | `Mutants` |
| The webhook shape | `Admission` |
| Entry point | `PolicyAsCodeDemo` |

## Requirements

1. The policy denies by default and treats a missing field as a denial.
2. A table of requests with expected rules is decided as expected.
3. The policy has Rego tests and mutation testing of them.
4. The same file serves an AdmissionReview and Conftest.
5. A new rule can be a warning before it is a denial.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 36 lines, identical on every run |
| Acts | 5 |
| Video | 05:55, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
