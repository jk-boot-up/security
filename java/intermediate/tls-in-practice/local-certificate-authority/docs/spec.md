# Spec — A Local Certificate Authority

## Purpose

Teach how a certificate authority works by building one with real X.509 and CRL bytes, then validating honest and dishonest certificates like a client, and comparing revocation with short lifetimes.

## Not in scope

- Production CA operations: key ceremonies, HSMs, auditing.
- OCSP and certificate transparency.
- ACME and the step-ca version: local-certificate-authority-with-step-ca.

## The problem

Teams run private CAs with long-lived certificates and no revocation plan, and clients that ignore the checks that make a certificate meaningful.

## The pieces

| Role | Class |
| --- | --- |
| DER encoding | `Der` |
| Certificate and CRL building | `Certificates` |
| The CA | `LocalCa` |
| The client's checks | `Verifier` |
| Entry point | `CaDemo` |

## Requirements

1. The CA issues X.509 v3 certificates with SAN, server-auth usage and basic constraints that the JDK reads and validates.
2. The client refuses wrong names, expired, unknown-issuer, forged, self-signed and leaf-signed certificates.
3. Revoked certificates are refused with a current list, and a stale or missing list is refused too.
4. Renewal keeps the key and names and changes serial and dates.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 33 lines, identical on every run |
| Acts | 5 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
