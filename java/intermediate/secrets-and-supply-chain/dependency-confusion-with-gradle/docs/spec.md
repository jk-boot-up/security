# Spec — Dependency Confusion with Gradle

## Purpose

Teach how Gradle resolves a name across repositories and how to restrict it with content filters and dependency verification.

## Not in scope

- Signing artifacts.
- Running a repository manager.
- Other build tools.

## The problem

Builds that list a public and a private repository can take an internal name from a public one.

## The pieces

| Role | Class |
| --- | --- |
| Repository | `Repo` |
| The two repositories | `Lab` |
| One Gradle build | `Build` |
| Entry point | `DependencyConfusionDemo` |

## Requirements

1. A Gradle build resolves from two repositories that remember every request.
2. A version range and declaration order can pick the attacker's package.
3. Content filters keep internal groups out of the public repository, which then receives no request for them.
4. Verification metadata refuses an unlisted version and different bytes.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 21 lines, identical on every run |
| Acts | 5 |
| Video | 05:16, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
