# Problem Statement

## The scenario

A bank writes security events to files on many machines, and an investigator needs to follow one address across all of them, and someone needs to be told when a guessing attack starts.

## The naive version

Log in to each machine and grep.

## What this project must deliver

- A real Loki and Grafana in Docker.
- Shipping, tenants and refusals.
- Questions in LogQL that the old logger could not answer.
- A rule that fires, and a rule that does not.
