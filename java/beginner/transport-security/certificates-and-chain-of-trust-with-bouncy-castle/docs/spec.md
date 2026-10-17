# Spec — Certificates and Chain of Trust with Bouncy Castle and OpenSSL

## Purpose

Teach how a small certificate authority works by building a chain with Bouncy Castle, limiting an intermediate with a name constraint, publishing signed revocation lists, handling a signing request, and checking everything with OpenSSL.

## Not in scope

- OCSP and certificate transparency.
- Automated issuance such as ACME.
- Hardware-protected authority keys.

## The problem

The plain project had to write certificates by hand, which hides what the standard extensions are for. A library lets the lesson show them by name, and a second tool shows they are standard.

## The pieces

| Role | Class |
| --- | --- |
| Builds certificates | `CertFactory` |
| The store's chain | `Pki` |
| Revocation lists | `Crls` |
| Chain validation | `ChainValidator` |
| Signing requests and policy | `SigningRequest` |
| PEM text | `Pem` |
| Entry point | `CertsBcDemo` |

## Requirements

1. A chain with correct basic constraints, key usages and key identifiers.
2. A name constraint refuses names outside the permitted domain.
3. A signed list revokes a serial, a list from another signer or no list leaves the status undetermined.
4. A request proves possession, a changed request fails, and the authority's policy refuses other domains.
5. OpenSSL verifies the chain, the lists and the request, and an OpenSSL authority can sign a Java request.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 32, all passing |
| Demo output | 36 lines, identical on every run |
| Acts | 5 |
| Video | 08:34, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
