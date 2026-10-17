# Spec — Backups and Recovery with an S3 Object Store

## Purpose

Teach immutable, least-privilege backups by running a real S3-compatible object store in Docker and showing versioning, Object Lock, a write-only identity, checksums and lifecycle rules with the AWS SDK for Java.

## Not in scope

- Backup scheduling and the contents of a backup: the plain project.
- Replication across sites and regions.
- Server-side encryption keys and key management services.

## The problem

Backups on the same account as the application are destroyed with it; learners need to see the features that prevent this working on a real store.

## The pieces

| Role | Class |
| --- | --- |
| The store in Docker | `ObjectStoreContainer` |
| An S3 client for one identity | `Store` |
| Entry point | `BackupsMinioDemo` |

## Requirements

1. An overwrite keeps the old version, and a delete only adds a marker.
2. A COMPLIANCE lock refuses deleting, bypassing and shortening, and a GOVERNANCE lock accepts a bypass.
3. The write-only identity can add objects to its bucket and nothing else.
4. A checksum that does not match the bytes is refused and leaves nothing behind.
5. No container or network is left after a run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 14, all passing |
| Demo output | 40 lines, identical on every run |
| Acts | 5 |
| Video | 07:31, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
