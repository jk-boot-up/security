# Cryptography

The tools underneath the rest of security: proving data was not changed, hiding it, sharing a key with a stranger, and proving who made something. Each project is checked against published standard test values.

## The projects

| # | Project | What it shows |
| --- | --- | --- |
| 1 | [hashing-and-integrity](hashing-and-integrity) | CRC-32, SHA-256 and HMAC against an attacker who changes an online store's invoices |
| 2 | [symmetric-encryption](symmetric-encryption) | AES in ECB and GCM modes, and the one mistake, a repeated nonce, that gives everything away |
| 3 | [symmetric-encryption-with-tink](symmetric-encryption-with-tink) | The same with Google Tink: one safe call, a keyset, rotation, and a keyset stored under a master key |
| 4 | [asymmetric-encryption](asymmetric-encryption) | RSA-OAEP, hybrid encryption and X25519 key agreement for a bank and a customer who have never met |
| 5 | [asymmetric-encryption-with-bouncy-castle](asymmetric-encryption-with-bouncy-castle) | Standard formats: PEM keys, an X.509 certificate and a CMS envelope that OpenSSL can open |
| 6 | [digital-signatures](digital-signatures) | Ed25519 signatures on a price list, and what a signature cannot do on its own |

A project with `-with-` in its name is the same lesson with a framework, a library or a local tool, and is its own self-contained project.

## Still to come

- keystore-basics (planned): where keys live

## How the module is judged

- **Break it first.** Each project shows the naive version failing before any control appears.
- **Every claim is a number.** Each failure and each fix prints an exact result that a test checks, against a golden output file.
- **The bill is part of the lesson.** The last act of every project shows what the control costs, or what it still leaves open.
- **A threat model for every project,** with a diagram, in `docs/threat-model.md`.

The shared decisions are in [`docs/spec.md`](docs/spec.md) and [`docs/implementation-plan.md`](docs/implementation-plan.md).
