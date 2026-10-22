# Spec — Container Hardening

## Purpose

Teach container hardening by asking thirteen attack questions of a container's settings, removing the attacks one setting at a time, classifying the result by Pod Security level, and showing what hardening leaves open.

## Not in scope

- A kernel or runtime simulation.
- Enforcing policies on a real cluster: container-hardening-with-docker-and-kind.
- Sandboxed runtimes (gVisor, Kata) and virtual machines.

## The problem

Containers run with defaults and tutorial flags, so one application flaw becomes a host compromise.

## The pieces

| Role | Class |
| --- | --- |
| The settings | `ContainerSpec`, `Seccomp` |
| The attacker model | `Attacker`, `Attack`, `Outcome` |
| The levels and renderings | `PodSecurityLevel`, `Renderer` |
| Entry point | `ContainerDemo` |

## Requirements

1. Docker's defaults allow 7 of the 13 attacks and are at the Baseline level.
2. The tutorial configuration allows all 13 and is Privileged, with the reasons listed.
3. Each hardening setting blocks the attacks it should, and a non-root user alone does not block privilege escalation.
4. The hardened container is Restricted and allows only reading the application's own secrets.
5. The rendered Docker flags and securityContext match the settings.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 54 lines, identical on every run |
| Acts | 5 |
| Video | 06:53, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
