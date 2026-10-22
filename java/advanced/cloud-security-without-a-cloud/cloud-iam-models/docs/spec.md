# Spec — Cloud IAM Models

## Purpose

Teach cloud permission evaluation, role trust and the confused deputy by writing the evaluator.

## Not in scope

- A complete implementation of any provider's service rules.
- Running against a cloud.
- Policy authoring tools.

## The problem

Cloud permissions come from several documents that interact, and mistakes in them are found by attackers.

## The pieces

| Role | Class |
| --- | --- |
| The policy language | `Policy`, `Conditions`, `Wild` |
| The evaluation | `Evaluator`, `Decision`, `Request`, `Principal` |
| Roles | `Role` |
| The escalation search | `Escalation` |
| Entry point | `IamModelDemo` |

## Requirements

1. An explicit deny in any policy denies.
2. Within one account an identity policy or a resource policy allows; across accounts both are needed.
3. Organization policies, boundaries and session policies only narrow.
4. A trust policy decides who may assume a role, with conditions.
5. The external id and the source ARN stop the confused deputy.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 19, all passing |
| Demo output | 50 lines, identical on every run |
| Acts | 5 |
| Video | 06:15, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
