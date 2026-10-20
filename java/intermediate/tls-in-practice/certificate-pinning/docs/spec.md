# Spec — Certificate Pinning and What It Costs

## Purpose

Teach certificate pinning by showing it stop an interceptor with a trusted root, then measuring the rotation outage it causes, and the choices that soften it.

## Not in scope

- Android or iOS APIs.
- Bypassing pins on a device.
- Certificate Transparency and CAA, which are mentioned as alternatives.

## The problem

Teams rely on the device's trust store, which an interceptor may have extended, or pin one key and cannot change it without breaking their users.

## The pieces

| Role | Class |
| --- | --- |
| Certificates and pins | `Pki`, `Der`, `Spki`, `PinPolicy` |
| The client and its outcomes | `PinnedClient`, `Outcome` |
| Rotation cost and header pins | `Fleet`, `PinStore` |
| Entry point | `PinningDemo` |

## Requirements

1. A pin adds to validation: a bad chain is refused whatever it contains, and a valid chain without a pinned key is refused by the pin.
2. An interceptor with a root the device trusts is stopped by a pin.
3. Renewing with a new key breaks a leaf pin and not an issuer pin; a backup pin allows a switch.
4. Report-only mode accepts and records.
5. Header-set pins lock out the genuine site until they expire.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 11, all passing |
| Demo output | 25 lines, identical on every run |
| Acts | 5 |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output and `animation.html`; `concept.toml`.
