# Spec — Crypto Agility

## Purpose

Teach crypto agility by building an inventory, a versioned envelope and a migration of a store of records across suites and keys.

## Not in scope

- Choosing the best algorithm.
- Hardware security modules.
- Key management services.

## The problem

Data is encrypted once, with nothing to say how, and the algorithm cannot be changed without knowing all its uses.

## The pieces

| Role | Class |
| --- | --- |
| The algorithm list | `Policy` |
| The scan of source | `Inventory` |
| The versioned value | `Envelope` |
| The keys | `KeyRing` |
| The records and the migration | `Store` |
| Entry point | `CryptoAgilityDemo` |

## Requirements

1. Every algorithm name in the source is listed with a status.
2. Every stored value carries an authenticated header with the suite and the key id.
3. The reader picks the suite from the header and obeys a policy.
4. Records can be migrated by read-repair and by batch without loss.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 15, all passing |
| Demo output | 28 lines, identical on every run |
| Acts | 5 |
| Video | 06:01, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
