# Spec — Runtime Security

## Purpose

Teach runtime detection by watching a real pod do normal work and then three things it must never do.

## Not in scope

- Responding to an alert (see the detection module).
- Falco's Kubernetes audit plugin.
- Seccomp and AppArmor profiles.

## The problem

Preventive controls are bypassed by a taken-over container, and without runtime detection nobody notices.

## The pieces

| Role | Class |
| --- | --- |
| The pods | `Pods` |
| The audit records | `Audit`, `Event` |
| The three rules | `Rules`, `Alert` |
| Falco | `Falco` |
| The audit sensor | `Sensor` |
| The cluster and the lab | `Cluster`, `Lab` |
| Entry point | `RuntimeSecurityDemo` |

## Requirements

1. Normal work of the service raises no alert.
2. A shell, a write outside the expected folders and a connection to an unknown address each raise one alert.
3. A refused write is still reported.
4. Falco accepts the rules file.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 27, all passing |
| Demo output | 28 lines, identical on every run |
| Acts | 6 |
| Video | 05:51, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
