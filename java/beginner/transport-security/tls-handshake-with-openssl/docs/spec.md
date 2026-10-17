# Spec — The TLS Handshake with OpenSSL

## Purpose

Teach how to check a TLS setup with a second implementation, by running a Java TLS server and client over real sockets, writing PEM files, and using openssl s_client and s_server as the other end in the integration tests.

## Not in scope

- Certificate authorities and chains: the next project.
- Mutual TLS with client certificates.
- TLS scanners.

## The problem

Learners see TLS working between two programs they wrote and do not know whether it is a standard or a private agreement.

## The pieces

| Role | Class |
| --- | --- |
| A TLS server | `TlsServer` |
| A checking TLS client | `TlsClient` |
| PEM files | `Pem` |
| Test certificates | `TestCertificates` |
| In-memory engines | `Connection`, `Wire` |
| Entry point | `TlsOpenSslDemo` |

## Requirements

1. The Java client and server agree TLS 1.3 over real sockets.
2. An untrusted certificate and a wrong name are refused by Java and by openssl.
3. Old versions are refused by Java and by openssl.
4. PEM files round-trip with the JDK and are read by openssl.
5. openssl is the client and the server in the integration tests, and the tests skip without OpenSSL 3.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 16, all passing |
| Demo output | 31 lines, identical on every run |
| Acts | 5 |
| Video | 07:21, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
