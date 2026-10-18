# Problem Statement

## The scenario

An online store ships a release jar. Customers, auditors and its own deployment need to know what is inside it, and that it was built by the store's build service from the
reviewed main branch, not from a fork, a branch or someone's laptop.

## The naive version

Sign the jar and trust any signed jar, with no record of what is inside it or where it was built from.

## What this project must deliver

- A CycloneDX 1.6 SBOM with package URLs and SHA-256 hashes, and checks that it matches the jar and its libraries.
- An in-toto statement with a SLSA Provenance v1 predicate, signed with Ed25519 in a DSSE envelope.
- A deployment gate that checks signature, subject digest, builder, repository and branch, and explains every refusal.
- SLSA build levels decided from the facts they depend on.
- A reproducible archive builder, and a rebuild checked against the provenance.
