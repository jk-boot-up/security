# Spec — Linux Hardening with Docker

## Purpose

Teach scanning a real container for common host misconfigurations and fixing them where they last.

## Not in scope

- Scanning vulnerabilities in packages.
- Runtime detection.
- Hardening the host kernel.

## The problem

Images collect settings that nobody chose, and fixes made in running containers do not last.

## The pieces

| Role | Class |
| --- | --- |
| The images | `Images` |
| A running container | `Host`, `Docker` |
| The rules | `Scanner`, `Finding` |
| Entry point | `HostScanDemo` |

## Requirements

1. A scan reads a running container with one script run as root inside it.
2. Each finding has a severity, a rule, a place, a reason and, when it is mechanical, a fix.
3. Fixes made in a running container are shown not to survive a new container.
4. The hardened image scans clean.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 14, all passing |
| Demo output | 57 lines, identical on every run |
| Acts | 5 |
| Video | 05:27, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
