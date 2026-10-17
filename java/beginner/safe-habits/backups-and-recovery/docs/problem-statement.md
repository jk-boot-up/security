# Problem Statement

## The scenario

An online store's orders, customers and stock live in files on one server. Disks fail, people delete things, and ransomware encrypts everything it can write to. The shop must be able to come back.

## The naive version

Copy the folder to another folder on the same server every night, and assume it works.

## What this project must deliver

- Full and incremental backups with a manifest of SHA-256 fingerprints, and a restore that rebuilds any day.
- Detection of a changed or missing stored file.
- A restore test that asks the application's own question, with a backup that passes its checks and fails the test.
- A 3-2-1 checker, and a retention policy on a simulated clock.
- A ransomware simulation against a writable copy, read-only files and a store that only adds versions.
- Recovery point and recovery time worked out from a timeline.
- An archive sealed with AES-256-GCM whose key lives apart.
