# Spec — Post-Quantum Cryptography

## Purpose

Teach post-quantum cryptography by using ML-KEM and ML-DSA from the JDK, measuring their cost and modelling the risk of recorded traffic.

## Not in scope

- Implementing the algorithms.
- Predicting when a quantum computer will exist.
- Post-quantum TLS configuration of a server.
- Quantum key distribution.

## The problem

Key exchange that is safe today is recorded and opened later, and nobody knows where the vulnerable algorithms are.

## The pieces

| Role | Class |
| --- | --- |
| Byte sizes of keys, ciphertexts, signatures | `Sizes` |
| Hybrid X25519 plus ML-KEM exchange | `Hybrid` |
| ML-DSA signing and verifying | `Signatures` |
| Mosca's inequality | `Mosca` |
| Inventory of the earlier projects | `Inventory` |
| Bouncy Castle cross-check | `Interop` |
| Entry point | `PostQuantumDemo` |

## Requirements

1. ML-KEM and ML-DSA are used through the JDK's own API.
2. Sizes are measured from real objects.
3. The hybrid key needs both secrets and the transcript.
4. A tampered ciphertext gives a different key and no error.
5. The inventory is counted from the source tree.
6. The output has no keys, random values or timings and is the same on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 17, all passing |
| Demo output | 74 lines, identical on every run |
| Acts | 5 |
| Video | 06:56, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
