# Spec — Ransomware Resilience

## Purpose

Teach immutable backups, isolated recovery and a timed restore exercise for a bank's ledger, and measure them against a ransomware simulation.

## Not in scope

- Real malware.
- Backup of databases with transaction logs.
- Cloud provider specific vault products.

## The problem

Backups are written by accounts that can delete them, copied without looking at them, and restored for the first time in a crisis.

## The pieces

| Role | Class |
| --- | --- |
| The S3-compatible store and its users | `Store` |
| Buckets, versions and error codes | `Bucket` |
| Access policies | `Policies` |
| The backup job | `BackupJob` |
| Canary and entropy checks | `Guard`, `Entropy` |
| The ransomware simulation | `Ransomware` |
| Restore and verification | `Restore` |
| RTO and RPO arithmetic | `Recovery` |

## Requirements

1. A plain, a versioned and a locked bucket are attacked with the backup credentials and the counts are printed.
2. Retention in COMPLIANCE mode cannot be shortened or removed by the administrator.
3. Writer and recovery credentials are separate and minimal.
4. A copy lives in a second store with unrelated credentials.
5. The ransomware simulation poisons a backup and the guard stops it.
6. The restore verifies SHA-256 sums, and RTO and RPO are computed, not timed.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 12, all passing |
| Demo output | 70 lines, identical on every run |
| Acts | 6 |
| Video | 07:17, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
