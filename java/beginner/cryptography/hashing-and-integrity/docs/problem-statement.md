# Problem Statement

## The scenario

An online store produces invoices and an installer, and sends them over channels and stores them in places that an attacker may be able to change. It needs to know whether
a copy is the one the store made.

## The naive version

Put a CRC or a hash next to the data. It catches accidents and misses an attacker, because whoever can change the data can change the check.

## What this project must deliver

- A CRC-32, SHA-256 and HMAC-SHA-256, each checked against published standard values.
- A demonstration that a CRC and a hash beside the data can be replaced by an attacker.
- A demonstration that a hash reaching the checker by another channel catches a tampered download.
- An HMAC check that refuses a changed invoice, a plain-hash tag and a tag made with a guessed key.
- The three problems around HMAC: ambiguous encoding, replay and a shared key.
- The same output every run, checked against a golden output file.
