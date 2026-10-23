# Problem Statement

## The scenario

A bank's services call each other, and a leaked key from any one of them must not be worth the whole bank.

## The naive version

Copy one static secret into every service and trust the caller's header for its name.

## What this project must deliver

- A shared secret that leaks, measured.
- SPIFFE IDs chosen by registration entries and the user a process runs as.
- Two Java services that verify each other against the trust bundle.
- Automatic rotation of 20 second SVIDs, counted.
- A stolen SVID and a process with no entry.
