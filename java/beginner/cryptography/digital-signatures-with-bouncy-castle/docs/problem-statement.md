# Problem Statement

## The scenario

An online bank signs the statements it sends to customers and to auditors. The signature must be checkable by software the bank did not write, years later, and the bank must not leak its signing key by a careless implementation detail.

## The naive version

Sign with whatever the JDK offers and invent a private envelope, or use ECDSA with a home-made random source. It works until another program has to check it, or until a weak random number leaks the key.

## What this project must deliver

- A statement signed in the standard CMS format, attached and detached, that verifies with its certificate and is refused when changed.
- ECDSA with random and with deterministic nonces, and a demonstration of what a reused nonce reveals.
- Ed25519, Ed448 and deterministic ECDSA checked against published vectors.
- A JWS token written by hand that reproduces the RFC example, with a verifier that decides the algorithm.
- PEM text armor for signed messages.
- OpenSSL verifying what Java wrote, and Java verifying what OpenSSL wrote.
