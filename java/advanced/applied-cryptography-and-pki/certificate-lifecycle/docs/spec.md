# Spec — Certificate Lifecycle

## Purpose

Teach the life of a certificate by running a private CA and measuring revocation, rotation and expiry with real tools.

## Not in scope

- Running a public CA.
- Certificate transparency.
- Hardware security modules.

## The problem

Revocation is trusted without asking what the client does when it cannot reach the answer, and renewal is trusted without watching it.

## The pieces

| Role | Class |
| --- | --- |
| The private CA | `Pki` |
| The OCSP responder with its switch | `Responder` |
| Chain and revocation checks | `Chains` |
| The stapling client | `StapleClient` |
| The CA that renews | `StepCa`, `Renewer` |
| The expiry monitor | `Fleet`, `ExpiryWatch` |
| Entry point | `CertificateLifecycleDemo` |

## Requirements

1. Certificates are issued by a root and an intermediate made with openssl and by step-ca.
2. CRL, OCSP and stapling are checked with hard fail and with soft fail.
3. Short-lived certificates are renewed after two thirds of their life with no failed handshake.
4. A missed renewal is shown to refuse every handshake, and a monitor alerts first.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 83 lines, identical on every run |
| Acts | 5 |
| Video | 06:10, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
