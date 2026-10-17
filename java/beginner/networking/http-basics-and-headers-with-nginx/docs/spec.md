# Spec — HTTP Basics and Security Headers with nginx

## Purpose

Teach how security headers, size limits and ambiguity checks behave in a real nginx by running it in Docker, auditing its answers with the same auditor as the plain project, and demonstrating always, inheritance and proxy_hide_header.

## Not in scope

- TLS configuration: the transport-security module.
- A web application firewall.
- nginx performance tuning.

## The problem

Header configuration is copied without being understood, and its failures are silent: the page still works.

## The pieces

| Role | Class |
| --- | --- |
| nginx configuration variants | `NginxConfig` |
| The container | `Nginx` |
| Raw requests and replies | `Probe` |
| Scoring a reply | `HeaderAudit` |
| Entry point | `HttpNginxDemo` |

## Requirements

1. Each variant's configuration contains exactly the directives it is meant to show.
2. Real nginx answers match every claim in the demo.
3. No container is left running after a demo or a test.
4. The demo prints only port-independent, timing-independent facts.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 29, all passing |
| Demo output | 25 lines, identical on every run |
| Acts | 5 |
| Video | 06:51, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
