# Problem Statement

## The scenario

An online store's internal services need certificates for their own names, and the store wants to issue and take them back itself.

## The naive version

Sign whatever is asked for, with one long-lived CA that can name anything, and rely on a revocation list that clients may never fetch.

## What this project must deliver

- A root and an issuing authority as real X.509 certificates with path length and name constraints.
- An issuer that checks proof of possession, names, allow-lists and lifetimes.
- Revocation lists read by the JDK's PKIX validator, with and without checking.
- A short-lived certificate that renews itself, and an inventory of expiries.
