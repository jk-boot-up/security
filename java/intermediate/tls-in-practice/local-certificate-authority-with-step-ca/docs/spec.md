# Spec — A Local Certificate Authority with step-ca

## Purpose

Teach running a real certificate authority with step-ca: issuance, policy, renewal and revocation, as seen from a client.

## Not in scope

- Running a production CA with hardware keys.
- A public ACME service.
- Device attestation.

## The problem

Teams need certificates for internal names and fall back to self-signed ones that no one can revoke or expire.

## The pieces

| Role | Class |
| --- | --- |
| The CA | `StepCa` |
| The client | `Pki` |
| Entry point | `StepCaDemo` |

## Requirements

1. A CA starts in a temporary folder on a free port and stops at the end.
2. A token works once and only for its name; the policy and the lifetime limit are enforced.
3. ACME issues for an allowed name and refuses others.
4. Renewal needs the certificate's own key.
5. A revoked certificate is refused by a client that checks a fresh signed list, and fails closed without one.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 34 lines, identical on every run |
| Acts | 5 |
| Video | 05:25, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
