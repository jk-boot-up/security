# Problem Statement

## The scenario

A bank must replace an algorithm in a database of customer records and in its backups.

## The naive version

Write a script that decrypts everything with the old code and encrypts it with the new, once.

## What this project must deliver

- An inventory of the algorithm names in the code.
- A versioned and authenticated envelope.
- A migration by read-repair and by batch, twice.
- Retiring a key, and the downgrade protection of a policy.
