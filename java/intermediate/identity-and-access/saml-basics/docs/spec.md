# Spec — SAML Single Sign-On and Signature Wrapping

## Purpose

Teach SAML validation by building signed responses and attacking three service providers with a self-signed response, an edited assertion and signature wrapping, then fixing the validation.

## Not in scope

- The full SAML schema, logout, encryption and metadata handling.
- The Keycloak version: the saml-basics-with-keycloak project.
- Writing your own SAML library for production.

## The problem

Developers check that a signature is valid and then read an assertion, without making sure that the signature covers the assertion they read and was made by the key they trust.

## The pieces

| Role | Class |
| --- | --- |
| The identity provider | `Idp` |
| Signature validation | `SignatureCheck` |
| The service providers | `ServiceProvider`, `TrustingServiceProvider`, `PinnedKeyServiceProvider`, `StrictServiceProvider` |
| XML helpers and attacks | `Xml`, `Attacks`, `SamlRefused` |
| Entry point | `SamlDemo` |

## Requirements

1. The trusting provider accepts a response signed with an attacker's key.
2. The pinned-key provider refuses it but is fooled by a wrapped response.
3. The strict provider refuses self-signed, edited and wrapped responses, and accepts a genuine one.
4. The strict provider checks issuer, time, audience, destination, request and replay, and refuses a DOCTYPE.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 10, all passing |
| Demo output | 22 lines, identical on every run |
| Acts | 4 |
| Video | not built yet |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
