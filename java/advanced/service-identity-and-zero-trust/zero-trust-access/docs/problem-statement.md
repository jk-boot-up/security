# Problem Statement

## The scenario

A bank's ledger API is reachable by anything inside the network and keeps no record of who did what.

## The naive version

Rely on the firewall and log the IP address.

## What this project must deliver

- A real identity provider issuing short-lived tokens.
- A proxy that validates the token and asks a policy engine.
- A default-deny policy with roles, device posture and time.
- One audit record per request, with no token in it.
