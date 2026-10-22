# Spec — TLS Servers in Practice with nginx and Caddy

## Purpose

Teach what nginx and Caddy do for TLS, as seen by a client, and what remains the operator's job.

## Not in scope

- Reverse proxying and load balancing.
- A public certificate authority.
- HTTP/2 and HTTP/3.

## The problem

TLS servers are configured by copying lines, and the effect of each line is rarely checked from the client.

## The pieces

| Role | Class |
| --- | --- |
| The client | `Https` |
| The local root | `LocalCa` |
| nginx | `Nginx` |
| Caddy | `Caddy` |
| Entry point | `TlsServersDemo` |

## Requirements

1. Both servers start on free loopback ports and stop at the end.
2. The client checks the certificate and the name, and reports what it got.
3. Each setting is changed alone and its effect shown.
4. A new certificate is shown to need a reload.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 32 lines, identical on every run |
| Acts | 5 |
| Video | 05:49, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
