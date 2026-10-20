# Spec — Incident Response: Prepare, Contain, Eradicate, Recover, Learn

## Purpose

Teach the incident lifecycle by replaying one scripted breach under three levels of preparation and response, and comparing the outcomes in a review.

## Not in scope

- A forensic tool, or legal and regulatory advice.
- Real malware analysis.
- Detection: the detecting-attacks project.

## The problem

Teams improvise during incidents, and the improvised steps destroy evidence, miss footholds and let the attacker return.

## The pieces

| Role | Class |
| --- | --- |
| Preparation | `Preparation` |
| The systems and attacker | `World` |
| The actions | `Responder` |
| The three responses | `Playbooks` |
| The review | `Outcome` |
| Entry point | `IncidentDemo` |

## Requirements

1. The attacker copies 1000 rows a minute from minute 20 until the route is cut.
2. Isolation keeps memory and stops the copying; a reboot destroys memory evidence and keeps the persistence.
3. Scoping needs central logs that reach back to the start, and an unknown account cannot be disabled.
4. A rebuild from an image removes the host's persistence; rebuilding by hand is slower.
5. Any way back in that remains is used within two hours.
6. The three responses give different reviews, and only the playbook recovers.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 15, all passing |
| Demo output | 48 lines, identical on every run |
| Acts | 6 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
