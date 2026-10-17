# Dependencies

This project uses two jars from Bouncy Castle, and one optional command-line tool.

## What Bouncy Castle (bcprov) is

an open-source cryptography library for Java. It supplies algorithms and formats that the JDK lacks, and registers itself as a security provider.

## What Bouncy Castle PKIX (bcpkix) is

the part that handles certificates, PEM, CMS and PKCS#8. It builds on bcprov.

## What OpenSSL is

a widely used cryptography toolkit and command-line program. The demo runs `openssl cms -decrypt` as an independent check. It is optional: without it, act four says it was skipped.

## Why this project uses them

The plain project shows the maths with the JDK alone. Real systems exchange keys and messages as files and in standard formats, which the JDK does not read or write. This project shows what a library adds for those formats, and uses a different program to prove the result is a standard and not a private convention.

## What to install

A JDK, and for act four and the integration tests, OpenSSL 3 on the PATH. Gradle downloads the Java libraries, and the versions are pinned:

| Tool | Version |
| --- | --- |
| Bouncy Castle (bcprov-jdk18on) | 1.86 |
| Bouncy Castle PKIX (bcpkix-jdk18on) | 1.86 |
| OpenSSL (optional, for act four) | 3.x |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- About 7 MB of library for a handful of format functions.
- A global provider registration that affects every library in the program.
- Act four and the integration tests need OpenSSL installed.
