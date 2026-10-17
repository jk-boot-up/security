# Problem Statement

## The scenario

A customer's browser connects to shop.example and receives a public key. The browser has no way to know, from the key alone, whether it belongs to the store or to someone sitting on the path.

## The naive version

Trust whatever key the server presents, or trust a certificate because it is well formed.

## What this project must deliver

- X.509 certificates built byte by byte, parsed and checked by the JDK.
- A chain of root, intermediate and server certificates validated with the JDK's PKIX validator on a fixed date.
- Refusals for expired, unknown-root, changed and overstepping certificates, each with the JDK's reason.
- A separate host name check, with wildcard rules.
- A revocation list, and a stolen-authority-key forgery that it stops.
