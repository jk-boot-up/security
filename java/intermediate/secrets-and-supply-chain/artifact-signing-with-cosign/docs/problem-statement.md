# Problem Statement

## The scenario

An online store deploys container images from a registry that several people and systems can push to, and wants to run only what its release pipeline built.

## The naive version

Deploy by tag and trust the registry.

## What this project must deliver

- A real registry and real images.
- Signing and verification with cosign 3 and a pinned key.
- Signed claims, key rotation and a thief with an old key.
- Signed provenance and a gate rule of its own.
