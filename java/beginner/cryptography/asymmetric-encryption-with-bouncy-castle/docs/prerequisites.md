# Prerequisites

## Required

- Java basics
- The plain asymmetric-encryption project, or at least what a public and a private key are
- OpenSSL 3 on the PATH, for act four and the integration tests (optional for the rest)

## Explicitly not required

- ASN.1, DER or any encoding details: they are hidden by the library
- A certificate authority: the certificate is self-signed
- Email or S/MIME software

## What you will need

- A JDK is optional: Gradle finds or downloads JDK 25 by itself. Everything else comes with the Gradle wrapper.
- On macOS, `brew install openssl` gives a version 3 `openssl`; most Linux systems already have one.
- About an hour for the session guide.
