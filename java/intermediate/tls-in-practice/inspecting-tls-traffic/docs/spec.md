# Spec — Inspecting TLS Traffic with a Key Log

## Purpose

Teach what a TLS capture shows and hides by building the key schedule, the records, the key log format and a decryptor, and by showing forward secrecy against a leaked server key.

## Not in scope

- A full TLS implementation: certificates, extensions and handshake encryption are left out.
- Wireshark: the with-Wireshark version uses tshark on a real capture.
- Man-in-the-middle proxies.

## The problem

Developers need to see encrypted traffic, and the tools that allow it leave behind files that can decrypt it again.

## The pieces

| Role | Class |
| --- | --- |
| Key schedule and records | `Hkdf`, `Secrets`, `Records` |
| Connections and captures | `Connection`, `Capture`, `Packet`, `LabelledRandom` |
| Key log and decryptor | `KeyLog`, `Decryptor`, `Hex` |
| Entry point | `TlsLogDemo` |

## Requirements

1. HKDF matches the RFC 5869 test vector, and records use the TLS 1.3 nonce and header rules.
2. A capture reveals no plaintext and no content type, and cannot be decrypted without keys.
3. A key log decrypts exactly the connections and directions it has lines for.
4. A leaked long-term key decrypts a static-RSA connection and nothing ephemeral.
5. The key log variable is refused when the environment is production.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 26 lines, identical on every run |
| Acts | 5 |
| Video | 06:44, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
