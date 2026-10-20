# Problem Statement

## The scenario

A shop runs internal services that talk TLS to each other, and wants certificates for its own names without buying them from a public CA.

## The naive version

Create one self-signed certificate and tell every client to trust it, forever.

## What this project must deliver

- A CA with a root key that signs real X.509 certificates built as DER.
- A client verifier using the JDK's PKIX validator with a fixed clock, revocation lists and name checks.
- Issue, renew, revoke and publish a list.
- A comparison of revocation with short-lived certificates.
