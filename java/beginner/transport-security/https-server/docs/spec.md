# Spec — An HTTPS Server, Put Together

## Purpose

Teach how the pieces of HTTPS fit together by running a real server on this machine, with a client that checks it, and a checklist that scores a careful and a careless configuration.

## Not in scope

- Getting a certificate from a public authority.
- Reverse proxies and load balancers: the tools project.
- Client certificates and mutual TLS.

## The problem

Learners who know what a certificate is still ship servers that are only partly secure, because they stop at the padlock.

## The pieces

| Role | Class |
| --- | --- |
| The shop (HTTPS and plain HTTP) | `ShopServer` |
| The verifying client | `ShopClient` |
| The checklist | `HttpsChecklist` |
| Header scoring | `HeaderAudit` |
| Test certificates | `TestCertificates` |
| Entry point | `HttpsDemo` |

## Requirements

1. A trusted request succeeds over TLS 1.3 with all five security headers.
2. The plain port redirects and sends no HSTS.
3. A Secure cookie is not sent over plain HTTP and an unflagged cookie is.
4. An untrusted certificate, a wrong name and a downgrade are each refused.
5. The checklist scores the careful shop 5 and the careless shop 1.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 30 lines, identical on every run |
| Acts | 5 |
| Video | 07:44, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
