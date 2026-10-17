# Spec — Hashing and Integrity

## Purpose

Teach the difference between a checksum, a cryptographic hash and a keyed hash by trying each against a tampering attacker on an online store's invoices and installer, and by showing the problems that remain around HMAC.

## Not in scope

- How the algorithms work inside.
- Choosing keys, storing them, and rotating them: the keystore project covers keys.
- Digital signatures: they are the next projects.

## The problem

Learners know "hash" and "checksum" but not what each can prove, and so use them where an attacker defeats them.

## The pieces

| Role | Class |
| --- | --- |
| Checksum | `Crc32Checksum` |
| Hash | `Sha256` |
| Keyed hash | `Hmac` |
| Hash kept beside the data | `InvoiceStore` |
| Tagged invoices | `SignedInvoices` and its `Encoding` |
| Entry point | `IntegrityDemo` |

## Requirements

1. CRC-32, SHA-256 and HMAC-SHA-256 match published standard values.
2. A hash stored beside the data is shown to be replaceable by an attacker who can write the data.
3. A tag made without the key is refused, whether it is old, a plain hash or made with a wrong key.
4. The two encodings show that joined fields are ambiguous and length-prefixed fields are not.
5. The demo output equals `docs/expected-output.txt` on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 25, all passing |
| Demo output | 32 lines, identical on every run |
| Acts | 5 |
| Video | 10:33, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
