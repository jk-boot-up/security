# Problem Statement

## The scenario

An online store needs certificates for its own services under a small authority it controls, with a limit on what that authority can ever vouch for, a way to withdraw a certificate, and a way for a service to request one without sharing its private key.

## The naive version

Generate a self-signed certificate for each service with a command someone found online, trust all of them everywhere, and hope nobody steals a key.

## What this project must deliver

- A root, an intermediate and a server certificate built with Bouncy Castle, with the extensions that set each one's role.
- A name constraint that the JDK and OpenSSL both enforce.
- Revocation lists signed by the issuer, and validation that uses them.
- A signing request that proves possession of a private key without revealing it, and an authority policy that checks the names.
- Independent checks by OpenSSL, including OpenSSL signing a request that Java made.
- Output that is the same on every run although the keys are random.
