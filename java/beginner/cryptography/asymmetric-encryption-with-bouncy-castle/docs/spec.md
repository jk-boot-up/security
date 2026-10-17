# Spec — Asymmetric Encryption with Bouncy Castle and OpenSSL

## Purpose

Teach what a library adds to the JDK's asymmetric encryption, the standard formats, by writing keys as PEM, making a certificate and sealing a CMS envelope with Bouncy Castle, and proving the result is a standard by having OpenSSL open it.

## Not in scope

- Certificate authorities, chains and revocation: the next modules.
- PGP: a different format, from the same library.
- Choosing key sizes and curves.

## The problem

Learners can do RSA with the JDK but cannot read, write or exchange the files that real systems use.

## The pieces

| Role | Class |
| --- | --- |
| PEM files | `Pem` |
| The certificate | `Certificates` |
| The CMS envelope | `CmsEnvelope` |
| The independent check | `OpenSsl` |
| Provider registration | `Providers` |
| Entry point | `BouncyCastleDemo` |

## Requirements

1. Keys and certificates round-trip through PEM, and an encrypted private key needs its password.
2. A CMS envelope opens with the recipient's key and not with another.
3. The key transport is OAEP by default, and the demo shows what the simple call picks.
4. The openssl command-line tool opens the envelope that the code wrote.
5. The demo output equals `docs/expected-output.txt` on a machine with openssl 3.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 20, all passing |
| Demo output | 26 lines, identical on every run |
| Acts | 5 |
| Video | 09:25, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
