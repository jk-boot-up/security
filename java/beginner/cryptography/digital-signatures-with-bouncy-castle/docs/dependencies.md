# Dependencies

This project uses two jars from Bouncy Castle, and one optional command-line tool.

## What Bouncy Castle (bcprov) is

an open-source cryptography library for Java. It supplies the Ed448 curve, deterministic ECDSA (HMacDSAKCalculator) and the lightweight signer classes that the JDK lacks.

## What Bouncy Castle PKIX (bcpkix) is

the part that handles certificates, PEM and CMS. It builds on bcprov.

## What OpenSSL is

a widely used cryptography toolkit and command-line program. The integration tests run `openssl cms -verify`, `openssl pkeyutl -verify` and `openssl dgst -verify` as independent checks. It is optional for the demo and required for the integration tests.

## Why this project uses them

The plain project shows signatures with the JDK alone. Real systems exchange signed messages in standard formats, which the JDK does not write. This project shows what a library adds, and uses a different program to prove the result is a standard and not a private convention.

## What to install

A JDK, and for the integration tests, OpenSSL 3 (the system openssl on macOS is LibreSSL, so use Homebrew's openssl@3). Gradle downloads the Java libraries, and the versions are pinned:

| Tool | Version |
| --- | --- |
| Bouncy Castle (bcprov-jdk18on) | 1.86 |
| Bouncy Castle PKIX (bcpkix-jdk18on) | 1.86 |
| OpenSSL (optional, for the integration tests) | 3.x |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- About 7 MB of library for a handful of format functions.
- A global provider registration that affects every library in the program.
- The integration tests need OpenSSL 3 installed.
