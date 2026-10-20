# Spec — Linux Hardening Checker

## Purpose

Teach host hardening by scanning a prepared Linux image for permission, account, SSH, kernel and port problems, applying the safe mechanical fixes, and showing the decisions left for a person.

## Not in scope

- A full CIS benchmark implementation.
- Running on Windows (the tests need Unix file modes).
- The Docker version: the linux-hardening-with-docker project.

## The problem

Images and hosts keep the settings that were convenient once, and nobody scans for them.

## The pieces

| Role | Class |
| --- | --- |
| The checker | `Scanner`, `Finding`, `Severity`, `Policy`, `Modes` |
| The prepared image | `Fixture` |
| Entry point | `LinuxHardeningDemo` |

## Requirements

1. The checker finds the planted permission, account, SSH, kernel and port problems and nothing on allow-listed items.
2. Findings are sorted worst first.
3. Permission fixes change only modes and leave no fixable finding; the other findings remain.
4. The hardened tree has no findings.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 16, all passing |
| Demo output | 46 lines, identical on every run |
| Acts | 4 |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output and `animation.html`; `concept.toml`.
