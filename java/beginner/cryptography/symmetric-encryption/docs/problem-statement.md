# Problem Statement

## The scenario

An online bank stores customers' statements on disks and in backups. Disks are stolen, files are changed, and the bank
must keep each statement secret and know when one has been tampered with.

## The naive version

Store the statements as they are, or encrypt them with ECB mode because it needs no nonce. The first is readable by anyone with the disk, and the
second leaks the pattern of the data.

## What this project must deliver

- AES in ECB mode, with the leak of its repeated blocks measured.
- AES-GCM with a fresh nonce for each message, a tag, and associated data.
- A demonstration that a flipped bit, a moved message and a wrong key are all refused.
- A demonstration of what a reused nonce gives an attacker.
- The costs: size, the limit of random nonces, and key storage.
- AES and GCM checked against published test values, and the same output every run.
