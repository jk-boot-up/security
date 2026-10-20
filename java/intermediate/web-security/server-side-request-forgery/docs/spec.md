# Spec — Server-Side Request Forgery (SSRF) and Its Defences

## Purpose

Teach SSRF by making a store's image import reach the metadata service and internal pages, then walking around a blocklist, and finally fixing it by checking and connecting to the real address.

## Not in scope

- A real network or HTTP client: the world is a table of responses.
- The Spring `RestClient` version: the server-side-request-forgery-with-spring-boot project.
- Cloud-specific metadata hardening.

## The problem

Developers fetch addresses on behalf of customers and trust the text of the address, so a customer can aim the server's own network position at its inside.

## The pieces

| Role | Class |
| --- | --- |
| The address parts | `Url` |
| Names and the network | `Dns`, `Network`, `Response` |
| The importers | `NaiveImporter`, `BlocklistImporter`, `SafeImporter` |
| The address rules | `AddressPolicy` |
| Entry point | `SsrfDemo` |

## Requirements

1. The naive importer reaches the metadata service, localhost and an internal page.
2. A blocklist of names is bypassed by five different spellings and routes.
3. The safe importer refuses all of them, follows only public redirects, and still imports honest images.
4. The safe importer connects only to the address it checked, so rebinding cannot change the target.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 31 lines, identical on every run |
| Acts | 5 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
