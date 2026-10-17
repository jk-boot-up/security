# Spec — HTTP Basics and Security Headers

## Purpose

Teach how HTTP messages are built and abused by writing a strict parser and writer for an online store, judging cookies, and auditing the security headers of a real response.

## Not in scope

- A complete HTTP implementation: chunked bodies, HTTP/2, connection reuse and caching.
- A framework's security configuration: the Spring Boot and nginx counterparts.
- TLS: the transport-security module.

## The problem

Learners see HTTP through frameworks and do not know what the server receives, what a proxy changes, or what a header does.

## The pieces

| Role | Class |
| --- | --- |
| Request model | `HttpRequest` |
| Strict parser | `HttpParser` |
| Response writer | `HttpResponse` |
| Cookie judge | `Cookie` |
| Header auditor | `HeaderAudit` |
| A real exchange | `LoopbackExchange` |
| Entry point | `HttpDemo` |

## Requirements

1. A request round-trips through the parser and the writer.
2. Ambiguous messages are refused: both lengths, differing lengths, a space before a colon, folded headers, bare line feeds, wrong body length.
3. A response with a line break in a header value or a bad header name is refused.
4. Cookies are judged for HttpOnly, Secure, SameSite and the __Host- prefix.
5. Headers are audited for value as well as presence.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 28, all passing |
| Demo output | 43 lines, identical on every run |
| Acts | 5 |
| Video | 08:02, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
