# Problem Statement

## The scenario

An online store builds a release jar and deploys it to production through mirrors, caches and build servers, any of which could change the file on the way.

## The naive version

Download the jar and run it, perhaps after comparing it with a checksum published in the same place.

## What this project must deliver

- A canonical release statement with the SHA-256 of the artifact, signed with Ed25519.
- A verifier that pins public keys per channel, compares digests in constant time and explains every refusal.
- Key rotation with an overlap, and revocation from a day onwards.
- A Merkle transparency log with signed heads, inclusion and consistency proofs, after RFC 9162.
- A deployment gate that requires a log entry and revokes by the log's day, and a monitor that reports releases the store never made.
