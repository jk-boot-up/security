# Problem Statement

## The scenario

An online store keeps its orders in a database and backs them up every night. Ransomware that reaches the servers also reaches whatever the servers can write to, and it deletes or encrypts the backups first.

## The naive version

Put the backups on a network share or a bucket that the backup job can read, write and delete, using the same account that runs the application.

## What this project must deliver

- A versioned bucket in which an overwrite and a delete are recoverable.
- A COMPLIANCE lock and a GOVERNANCE lock, and what each refuses.
- A backup identity whose policy has one action, created with the store's admin tool.
- An upload whose SHA-256 checksum is verified by the store.
- A lifecycle rule, and an honest list of costs.
- A container and network that are always removed, even when a test fails.
