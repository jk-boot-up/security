# Spec — Reverse Engineering Fundamentals

## Purpose

Teach the fundamentals of reverse engineering Java by taking apart a small client and its obfuscated copy.

## Not in scope

- Native code and Ghidra.
- Breaking the protection of other people's software.
- Commercial obfuscators.

## The problem

Teams ship checks and secrets in client code and assume that they cannot be read or changed.

## The pieces

| Role | Class |
| --- | --- |
| The program under study | `Sample` |
| Reading class files | `Pool` |
| Changing class files | `Patcher` |
| javap and CFR | `Tools` |
| Shell commands | `Sh` |
| Entry point | `ReverseEngineeringDemo` |

## Requirements

1. The sample compiles in the demo from source in the repository.
2. Strings, names and the key check are found from the class files.
3. A patched copy accepts any key in the plain and in the obfuscated sample.
4. The decompiler is downloaded once and checked against its SHA-256.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 8, all passing |
| Demo output | 23 lines, identical on every run |
| Acts | 5 |
| Video | 05:22, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
