# Spec — Symmetric Encryption with Google Tink

## Purpose

Teach what a safe encryption library does by repeating the plain symmetric-encryption story with Google's Tink: one call, key rotation, envelope encryption under a master key, and a clear list of what is still the caller's job.

## Not in scope

- Tink's other primitives: signatures, MACs and hybrid encryption.
- Key management services: the master key is a second demo keyset.
- Android or Google Cloud specifics.

## The problem

Learners who have seen the plain version need to see how a library removes its sharp edges, and which edges remain.

## The pieces

| Role | Class |
| --- | --- |
| The bank's encryption | `Bank` |
| Fixed keysets for the demo | `DemoKeysets` |
| Entry point | `TinkDemo` |

## Requirements

1. Sealing the same data twice gives different bytes, and each message starts with the key's version and id.
2. A changed bit, a moved message and a wrong key are all refused.
3. After rotation, old messages open and new ones use the new key; after removal, old ones do not open.
4. A keyset written encrypted does not contain the key bytes and reads back only with the master key.
5. The demo output equals `docs/expected-output.txt` on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 28 lines, identical on every run |
| Acts | 5 |
| Video | 09:15, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
