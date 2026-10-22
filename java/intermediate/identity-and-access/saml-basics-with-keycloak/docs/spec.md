# Spec — SAML Basics with Keycloak

## Purpose

Teach what a SAML service provider must check, with a real library and a real identity provider.

## Not in scope

- Single logout.
- Encrypted assertions.
- Running an identity provider in production.

## The problem

Teams add SAML single sign-on with a library and never find out which attacks the library stops and which operational risks it leaves.

## The pieces

| Role | Class |
| --- | --- |
| The identity provider | `KeycloakLab`, `infra/*-realm.json` |
| The shop | `ShopApp`, `ShopSecurity`, `ShopController`, `Refusals` |
| The browser | `Shopper` |
| The attacker's tools | `SamlTools` |
| Entry point | `SamlKeycloakDemo` |

## Requirements

1. The shop signs a shopper in only for a response signed by the key in the bank's metadata.
2. An edited, self-signed, wrapped, unsigned or foreign-signed response is refused.
3. A DOCTYPE is refused.
4. A response is accepted once, for the request the shop made.
5. An expired assertion and one for another audience are refused.
6. A key rotation at the identity provider is an outage until the metadata is read again.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 21, all passing |
| Demo output | 31 lines, identical on every run |
| Acts | 5 |
| Video | 06:36, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
