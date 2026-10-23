# Spec — Zero Trust Access

## Purpose

Teach the three steps of zero trust access on a bank's ledger API: authenticate, authorize and log every request with a real identity provider and policy engine.

## Not in scope

- Running Envoy or oauth2-proxy.
- A real device management system.
- Single sign-on in a browser.

## The problem

Internal APIs answer anyone on the network and cannot say who did what.

## The pieces

| Role | Class |
| --- | --- |
| The identity-aware proxy | `Proxy` |
| Token validation | `Jwt`, `Jwks` |
| The identity provider | `Keycloak` |
| The policy engine | `Opa`, `policy/ledger.rego` |
| The ledger | `Ledger` |
| The audit log | `AuditLog` |

## Requirements

1. Every request is authenticated, authorized and logged.
2. A token is accepted only with a good signature, issuer, audience and expiry.
3. The policy denies by default and names its reasons.
4. Every request has exactly one audit record and no record holds a token.
5. A request that skips the proxy is refused by the ledger.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 78 lines, identical on every run |
| Acts | 5 |
| Video | 06:34, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
