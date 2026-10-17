# Spec — Keystores and Key Management Basics

## Purpose

Teach where keys should live by putting an online bank's statement key in the code, in a file and in a PKCS12 keystore, deriving keys with PBKDF2 and HKDF, and rotating a key using key names inside the messages.

## Not in scope

- Key management services and hardware security modules: they are described, not run.
- TLS keystores and certificates: the transport-security module.
- Secret scanning in a repository's history.

## The problem

Learners know to encrypt but not where the key goes, and the answer decides whether the encryption was worth doing.

## The pieces

| Role | Class |
| --- | --- |
| The hard-coded key | `HardCodedKey` |
| Key files | `KeyFile` |
| The keystore | `KeystoreVault` |
| Derived keys | `Derivation` |
| Messages that name their key | `Messages` |
| Entry point | `KeystoreDemo` |

## Requirements

1. A hard-coded key is found in the compiled class, and a file's permissions are shown.
2. A keystore file shows neither the key nor its name, and refuses a wrong password or a changed file.
3. PBKDF2 and HKDF match published values.
4. A message names its key, old and new messages open while both keys exist, and an old message is refused once its key is removed.
5. The demo prints the same output on every run although its keys are random.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 25, all passing |
| Demo output | 33 lines, identical on every run |
| Acts | 5 |
| Video | 09:24, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
