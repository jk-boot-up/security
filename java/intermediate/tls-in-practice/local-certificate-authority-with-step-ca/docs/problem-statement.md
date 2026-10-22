# Problem Statement

## The scenario

An online store runs many internal services that need certificates, and no public authority will certify its internal names.

## The naive version

Self-signed certificates that every client is told to trust, with no expiry.

## What this project must deliver

- A real step-ca with a token provisioner and an ACME provisioner.
- A policy for names and a limit for lifetimes.
- Renewal and revocation, with clients written with the JDK.
- What a revocation list does not do.
