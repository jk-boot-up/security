# Spec — Least Privilege with Terraform and LocalStack

## Purpose

Teach least privilege for cloud permissions by writing IAM policies in Terraform for an online store, applying them to a local emulator, and evaluating, counting and linting the same JSON in Java.

## Not in scope

- Enforcing policies: that needs a real account or a paid emulator.
- Conditions, resource policies and organisation rules.
- Terraform state storage and modules.

## The problem

Cloud permissions are easy to write too wide, and hard to see once written.

## The pieces

| Role | Class |
| --- | --- |
| Glob matching | `Glob` |
| Policy documents | `PolicyDocument` |
| Allow, deny and boundary rules | `Evaluator` |
| The 25 things | `Catalogue` |
| The linter | `Linter` |
| The fixtures | `Fixtures` |
| Entry point | `LeastPrivilegeTerraformDemo` |

## Requirements

1. Wildcard matching handles star and question mark, with case rules for actions and resources.
2. An explicit deny wins in any order, and a boundary only narrows.
3. Blast radius is counted over the fixed catalogue.
4. The linter flags star actions, star resources, service stars, NotAction and escalation actions.
5. The Terraform code applies to the emulator, and what it stores equals the fixtures.
6. The demo prints the same output on every run and needs no tool.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 25, all passing |
| Demo output | 37 lines, identical on every run |
| Acts | 5 |
| Video | 07:05, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
