# Problem Statement

## The scenario

An online store's CI build pulls internal libraries from a private repository and open-source libraries from a public registry, and runs install scripts.

## The naive version

Configure both repositories and let the tool find the best version: the private names are ours, so nobody else can have them.

## What this project must deliver

- Repositories, artifacts with install scripts, versions and ranges.
- Resolvers for highest-version, first-repository and content-filtered behaviour.
- A lockfile and a verifying resolver using SHA-256 digests.
- A build that reports the scripts it ran, and a name watch.
