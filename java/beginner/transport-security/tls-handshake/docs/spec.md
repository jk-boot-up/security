# Spec — The TLS Handshake

## Purpose

Teach what the TLS handshake and record layer do by running two real TLS engines against each other in memory, with a sniffer that records the wire, a trusted and an untrusted certificate, a tampered record and old protocol versions.

## Not in scope

- Certificate authorities and chains: the next project.
- Writing the protocol: the JDK engine does that.
- Sockets, HTTP and servers: the https-server project.

## The problem

Learners know HTTPS is safe but not why, and cannot say what it protects against or what it leaves visible.

## The pieces

| Role | Class |
| --- | --- |
| The two engines and the loop | `Connection` |
| The sniffer's view | `Wire` |
| Test certificates | `TestCertificates` |
| Entry point | `TlsDemo` |

## Requirements

1. The handshake succeeds with TLS 1.3 and the wire is recorded.
2. An untrusted certificate and a wrong name are refused.
3. A record with any changed byte is refused.
4. TLS 1.0 is refused, TLS 1.2 works unless the server requires 1.3.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 13, all passing |
| Demo output | 28 lines, identical on every run |
| Acts | 5 |
| Video | 07:21, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
