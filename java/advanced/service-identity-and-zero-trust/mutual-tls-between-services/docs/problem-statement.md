# Problem Statement

## The scenario

A bank wants its ledger to know which service is calling, and to refuse the rest, even from inside the network.

## The naive version

Allow the internal network and give every service the same API key.

## What this project must deliver

- Client certificates from a private CA.
- Authorization by the identity in the certificate.
- Short-lived certificates renewed without downtime.
- A look at what a revoked certificate can still do.
