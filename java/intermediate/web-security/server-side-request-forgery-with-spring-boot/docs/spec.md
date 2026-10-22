# Spec — Server-Side Request Forgery with Spring Boot

## Purpose

Teach server-side request forgery on a real Spring Boot application with RestClient: the naive importer, a blocklist, and the address-pinning fix with limits.

## Not in scope

- A real cloud metadata service.
- HTTPS and certificate handling.
- Network-level egress rules, which are described only.

## The problem

Servers fetch customer-supplied addresses from inside a trusted network, and checks on the text of the address do not match what the network connects to.

## The pieces

| Role | Class |
| --- | --- |
| Importers | `NaiveImporter`, `BlocklistImporter`, `GuardedImporter`, `TwoStepImporter` |
| Judging and parsing | `AddressPolicy`, `Target` |
| Fetching | `Fetcher`, `Dns`, `Router` |
| The invented internet | `Internet` |
| The web layer and entry point | `ImportController`, `SsrfSpringDemo` |

## Requirements

1. The naive importer reaches the metadata address, localhost and the office page.
2. A blocklist of names is beaten by a decimal spelling, a short loopback, a mapped IPv6 address, a name that points inside and a redirect.
3. The guarded importer refuses all of them, judges each redirect and refuses user info and odd ports.
4. The guarded importer connects to the address it judged and so cannot be rebound.
5. A check followed by a second lookup is beaten by a rebinding name server.
6. Type, size and time are limited.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 33, all passing |
| Demo output | 35 lines, identical on every run |
| Acts | 5 |
| Video | 06:33, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
