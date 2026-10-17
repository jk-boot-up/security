# Spec — Backups and Recovery

## Purpose

Teach backups as a security control by making full and incremental backups with a manifest, testing a restore, grading the 3-2-1 rule, pruning by retention, simulating ransomware, calculating recovery objectives and sealing the archive.

## Not in scope

- Backup products, schedulers and cloud services: described, not run.
- Database consistency and point-in-time recovery.
- A real write-once store: modelled by a store that only adds versions.

## The problem

Learners know backups matter but not what makes one trustworthy, or how ransomware defeats the usual setup.

## The pieces

| Role | Class |
| --- | --- |
| Fingerprints and manifests | `Sha256`, `Manifest` |
| Full and incremental snapshots | `BackupStore` |
| The restore test | `RestoreTest` |
| The 3-2-1 rule | `ThreeTwoOne` |
| Retention | `Retention` |
| Ransomware and a versioned store | `Ransomware` |
| Recovery objectives | `Objectives` |
| The sealed archive | `BackupVault` |
| Entry point | `BackupsDemo` |

## Requirements

1. Incremental snapshots store only changes and any day can be restored from the chain.
2. A changed or missing stored file is found from the manifest.
3. A backup missing the orders file passes the manifest and fails the restore test.
4. The 3-2-1 rule and the retention policy give the numbers in the demo.
5. A store that only adds versions restores the files from before an attack.
6. A wrong key does not open the archive.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 18, all passing |
| Demo output | 39 lines, identical on every run |
| Acts | 5 |
| Video | 08:28, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
