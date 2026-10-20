# Spec — Dependency Confusion

## Purpose

Teach dependency confusion by registering a public package under a private name and watching five resolvers, then fixing the build with name-based routing, locked digests and a name watch.

## Not in scope

- A real registry, build tool or network.
- Typosquatting and malicious maintainers of legitimate packages.
- The Gradle version: the dependency-confusion-with-gradle project.

## The problem

Build tools choose between repositories by rules that were designed for convenience, and an attacker can satisfy those rules from outside.

## The pieces

| Role | Class |
| --- | --- |
| Packages and repositories | `Version`, `VersionRange`, `Artifact`, `Repository`, `Resolved` |
| Resolvers | `Resolver`, `HighestVersionResolver`, `FirstRepositoryResolver`, `ContentFilteringResolver`, `VerifyingResolver` |
| Locking and detection | `Lockfile`, `NameWatch` |
| The build | `Build`, `ResolutionException` |
| Entry point | `DepConfusionDemo` |

## Requirements

1. Highest-version-wins installs a public package that has an internal name and a higher version.
2. Repository order does not protect a name missing from the private repository or a public-first order, even with an exact pinned version.
3. Content filtering supplies internal names only from the private repository, fails closed when missing, and never asks the private repository for public names.
4. A lockfile with digests rejects a different source, version or contents.
5. A name watch lists internal names that the public registry has.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 51 lines, identical on every run |
| Acts | 5 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
