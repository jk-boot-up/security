# Dependencies

This project uses two jars from Bouncy Castle, and one optional command-line tool for the integration tests.

## What Bouncy Castle (bcprov) is

an open-source cryptography library for Java. The certificate builders use it for algorithms and the encoding of names and extensions.

## What Bouncy Castle PKIX (bcpkix) is

the part that builds certificates, lists and signing requests, and reads and writes PEM. It builds on bcprov.

## What OpenSSL is

a widely used cryptography toolkit and command-line program. The integration tests run `openssl verify`, `openssl x509`, `openssl crl`, `openssl req` and `openssl x509 -req` as independent checks. It is optional: the demo and ordinary tests do not need it.

## Why this project uses them

The JDK validates certificates but cannot create them, and reading its own certificates back proves little. This project builds real certificates with a library, and uses a different program to prove that they are standard and not a private convention.

## What to install

A JDK, and for the integration tests, OpenSSL 3 on the PATH. Gradle downloads the Java libraries, and the versions are pinned:

| Tool | Version |
| --- | --- |
| Bouncy Castle (bcprov-jdk18on) | 1.86 |
| Bouncy Castle PKIX (bcpkix-jdk18on) | 1.86 |
| OpenSSL (optional, for the integration tests) | 3.x |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- A few megabytes of library for a handful of structures.
- The integration tests need OpenSSL 3 installed.
- Extensions are easy to write and easy to get quietly wrong.
