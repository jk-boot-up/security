# Problem Statement

## The scenario

A bank wants its ledger backups to survive an attacker who has the backup credentials, and wants to know how long and how much data a recovery costs.

## The naive version

Write the backups to a bucket with a service account that can do everything and hope.

## What this project must deliver

- A measurement of what each layer survives.
- A lock that the administrator cannot shorten.
- Separate credentials and a copy in a second store.
- A guard against poisoned backups, and a restore with a computed RTO and RPO.
