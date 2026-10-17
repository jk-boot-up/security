# Spec — Password Attacks and Defences with nginx

## Purpose

Teach how a real rate limit works and where it fails, by putting a bank's login behind a real nginx container and counting what twenty guesses achieve with five different keys.

## Not in scope

- A production nginx configuration: TLS, timeouts and logging are left out.
- Real-time rates: the rate is deliberately slow so counts are repeatable.
- Distributed attackers: the client is one machine.

## The problem

Learners know a rate limit is good but not that what it counts decides whether it works.

## The pieces

| Role | Class |
| --- | --- |
| The configuration | `NginxConfig` |
| The container | `Nginx` |
| The attacker | `Attacker` and `Tally` |
| Entry point | `NginxAttacksDemo` |

## Requirements

1. The unit tests need no Docker; the container tests are tagged `docker`.
2. Every nginx configuration is generated from `NginxConfig`, and `infra/nginx-limit-by-address.conf` equals its output.
3. The nginx image is pinned to an exact version.
4. The demo output equals `docs/expected-output.txt` on every run.
5. A client-written header is never the key of the address limit.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 14 lines, identical on every run |
| Acts | 5 |
| Video | 08:25, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
