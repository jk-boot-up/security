# Spec — Certificates and the Chain of Trust

## Purpose

Teach what a certificate and a chain of trust are by building X.509 certificates, validating a chain with the JDK, and showing each way validation can fail.

## Not in scope

- Running a real certificate authority.
- Certificate formats such as PEM and PKCS12: the next project and the keystore project.
- OCSP and CRL protocols.

## The problem

Learners see the padlock but not what stands behind it, so certificate errors and outages are mysteries.

## The pieces

| Role | Class |
| --- | --- |
| DER encoding | `Der` |
| Certificate issuing | `CertBuilder` |
| The store's certificates | `Pki` |
| Chain validation | `ChainValidator` |
| Host names | `HostName` |
| Revocation | `RevocationList` |
| Entry point | `CertsDemo` |

## Requirements

1. The DER encodings match published values and the JDK parses every certificate made.
2. A good chain is accepted, and an expired, unknown-root, changed or overstepping chain is refused with the JDK's reason.
3. The host name check handles exact names and one-label wildcards.
4. A revoked serial is refused, and a stolen authority key is shown to forge accepted certificates until revoked.
5. The demo prints the same output on every run although its keys are random.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 21, all passing |
| Demo output | 30 lines, identical on every run |
| Acts | 5 |
| Video | 07:13, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
