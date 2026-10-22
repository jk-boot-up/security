# Spec — Container Hardening with Docker and kind

## Purpose

Teach container hardening by measuring: real attempts inside real containers and a real cluster's admission.

## Not in scope

- Container escapes.
- Network policies.
- Image vulnerability scanning.

## The problem

Containers are started with options copied from tutorials, and nobody measures what an attacker inside could do.

## The pieces

| Role | Class |
| --- | --- |
| The image and containers | `Image`, `Run` |
| The attempts | `Probes` |
| The cluster | `Cluster`, `Pods` |
| Entry point | `ContainerHardeningDemo` |

## Requirements

1. Thirteen attempts run inside a container as the user it runs as.
2. Each stage of options is measured, not scored.
3. A real kind cluster enforces the three standards and judges three pods.
4. Pods that are admitted are probed from inside.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 15, all passing |
| Demo output | 52 lines, identical on every run |
| Acts | 5 |
| Video | 06:10, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
