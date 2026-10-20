# Spec — Finding TLS Misconfigurations

## Purpose

Teach TLS configuration review by writing a checker with stable rules and a grade, running it against four fixed servers, and moving a legacy server to a published policy.

## Not in scope

- Network scanning: the profiles are fixed data.
- testssl.sh: the tls-misconfigurations-with-testssl project.
- TLS implementation attacks beyond configuration (the library must be patched).

## The problem

Servers are set up once and never reviewed, so old settings, expiring certificates and missing headers stay until a user or an auditor finds them.

## The pieces

| Role | Class |
| --- | --- |
| The profile | `ServerProfile`, `CertFacts`, `HttpFacts` |
| The checker and its report | `Checker`, `Finding`, `Severity`, `Report`, `CipherSuites` |
| Policies | `Policy` |
| Demo servers | `Profiles` |
| Entry point | `TlsCheckDemo` |

## Requirements

1. Each rule has a stable id and severity, and reports are sorted worst first.
2. Old protocols, broken and non-forward-secret suites, certificate faults and HTTP faults are found, with boundary tests.
3. Wildcards match one label only.
4. Grades follow the score, and any critical finding gives F while any high finding caps the grade at C.
5. A policy fixes protocol and suite findings and nothing else, and generates an nginx fragment.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 44 lines, identical on every run |
| Acts | 4 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
