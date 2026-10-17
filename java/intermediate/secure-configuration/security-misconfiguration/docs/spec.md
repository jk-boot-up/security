# Spec — Security Misconfiguration

## Purpose

Teach security misconfiguration by starting a real store with its shipped settings, checking it from outside, hardening it with secure defaults, a startup gate and a drift check, and showing the smaller leaks.

## Not in scope

- Framework-specific settings such as Spring Boot's actuator: the tools version.
- Operating system hardening: the platform-hardening module.
- TLS settings: the tls-in-practice module.

## The problem

Teams fix code and forget settings; most misconfigurations are defaults that nobody changed, and they are found by scanners first.

## The pieces

| Role | Class |
| --- | --- |
| Settings | `StoreConfig`, `Profiles` |
| The store | `StoreServer` |
| Outside checks and score | `ConfigChecker` |
| Startup gate | `StartupGate` |
| Drift check | `ProfileDrift` |
| Settings file permissions | `ConfigFile` |
| Secret comparison | `Secrets` |
| Entry point | `MisconfigDemo` |

## Requirements

1. The shipped store fails all eight checks and the hardened store passes them all.
2. The startup gate refuses a production store with an empty, short or default admin password, a debugging switch or a test setting.
3. The drift check finds development values, development-only settings and unreviewed settings.
4. Errors show a reference and no internals; TRACE and unknown methods are refused.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 20, all passing |
| Demo output | 37 lines, identical on every run |
| Acts | 5 |
| Video | 07:42, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
