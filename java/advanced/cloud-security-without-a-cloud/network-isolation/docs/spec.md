# Spec — Network Isolation

## Purpose

Teach network segmentation by measuring reachability on real networks against a written intent.

## Not in scope

- Firewalls on the host.
- Kubernetes network policies.
- Authentication between services.

## The problem

Services share networks that nobody drew carefully, so a taken-over service reaches things it should not.

## The pieces

| Role | Class |
| --- | --- |
| The shop | `Service` |
| The networks and containers | `Lab` |
| The design | `Intent` |
| Entry point | `NetworkIsolationDemo` |

## Requirements

1. Reachability is measured from inside containers, by name and by address.
2. The intent is a table of allowed pairs and a layout can be built from it.
3. The measurement is compared with the intent for extra and missing pairs.
4. Published ports and egress are measured.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 8, all passing |
| Demo output | 24 lines, identical on every run |
| Acts | 5 |
| Video | 05:51, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
