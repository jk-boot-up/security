# Problem Statement

## The scenario

An online store's servers need a database password and a payment provider's API key. Developers, build servers, container images and the
running application all handle them, and any of them can leak.

## The naive version

Put the password in the configuration file, the key in an environment variable, and copy both into the image so that it works everywhere.

## What this project must deliver

- A small secret scanner using setting names and entropy, run over a sample repository.
- Demonstrations of the environment-variable and image-layer leaks.
- Run-time injection from owner-only files, a Secret type that never prints, and wiping after use.
- A model secrets service with per-application leased logins, renewal limits, expiry, revocation and an audit trail.
- A key rotation with two versions during the changeover, and the blast radius of a leak measured by its lease.
