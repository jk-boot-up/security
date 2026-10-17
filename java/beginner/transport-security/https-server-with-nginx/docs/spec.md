# Spec — HTTPS Server with nginx

## Purpose

Teach HTTPS at the front door by running real nginx in Docker with a careful TLS configuration, checking it from a Java client that verifies the chain and the host name, and showing the traps: old protocols, a forgeable forwarded header and a missing certificate.

## Not in scope

- Certificates from a public authority and OCSP stapling: described, not run.
- Load balancing and caching.
- TLS between nginx and the application.

## The problem

TLS is usually ended by a web server in front of the application, and copied configurations carry hidden weaknesses.

## The pieces

| Role | Class |
| --- | --- |
| nginx configurations | `NginxConfig` |
| The container | `Nginx` |
| The checking client | `TlsClient` |
| The demo certificates | `Pki` |
| Configuration score | `ConfigScore` |
| Raw plain-port requests | `Probe` |
| Header auditor | `HeaderAudit` |
| Entry point | `HttpsNginxDemo` |

## Requirements

1. The careful configuration gives TLS 1.3 with HTTP/2 and five of five headers.
2. A TLS 1.2 client is refused unless the configuration allows it.
3. The plain port redirects with 301 and sends no HSTS.
4. An untrusted root, a wrong name and an impostor certificate are refused.
5. With mutual TLS a client without a certificate is refused and the identity is set by nginx.
6. Passing on the client's forwarded protocol is shown to deceive the application.
7. The configuration is tested with nginx -t and scored.
8. No containers are left behind.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 31, all passing |
| Demo output | 32 lines, identical on every run |
| Acts | 5 |
| Video | 08:41, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
