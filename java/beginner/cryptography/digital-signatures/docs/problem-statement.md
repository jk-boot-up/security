# Problem Statement

## The scenario

An online store publishes a price list that several services rely on. Each service must be sure that a list came from the store, and none of them should be able to make a list that looks as if it did.

## The naive version

Tag the list with a shared HMAC key. Every service that checks can also forge, so a single compromised or careless service can publish its own prices.

## What this project must deliver

- A demonstration of the forgery that a shared key allows.
- Ed25519 signing and verification, checked against the published test vectors of RFC 8032.
- A fixed, unambiguous encoding of the price list, and a label of purpose signed with it.
- Version and expiry inside the signed bytes, and a checkout that refuses old and expired lists.
- A demonstration of the two things a signature cannot do: say whose key it is, and survive theft of the key.
- The same output on every run, checked against a golden output file.
