# Spec — Hardware-Backed Keys

## Purpose

Teach hardware-backed keys by generating a key on a PKCS#11 token, signing with it from Java and measuring what a token does and does not protect.

## Not in scope

- Certifying anything to FIPS 140.
- Measuring real HSM tamper resistance.
- Key ceremonies with people and witnesses.

## The problem

Signing keys live in files and are copied, and tokens are trusted to protect more than they do.

## The pieces

| Role | Class |
| --- | --- |
| Token, scratch directory, provider | `Lab` |
| Raw PKCS#11 calls | `Raw` |
| Key ceremony | `Provisioning` |
| Login and signing | `TokenAccess` |
| Rate, approvers | `SigningService` |
| Hash-chained log | `AuditLog` |
| Code in the process | `Malware` |
| Entry point | `HardwareBackedKeysDemo` |

## Requirements

1. The key is generated on the token, sensitive and not extractable.
2. Java signs through the PKCS11 KeyStore and verifies with the exported public key.
3. Five ways of exporting the key are tried and their answers printed.
4. Wrong PINs, a software key, a copied token and a compromised host are measured.
5. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 21, all passing |
| Demo output | 59 lines, identical on every run |
| Acts | 5 |
| Video | 05:40, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
