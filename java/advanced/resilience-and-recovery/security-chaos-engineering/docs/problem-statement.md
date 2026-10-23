# Problem Statement

## The scenario

A bank wants to know what its payments service does when the controls it depends on fail, before an attacker or an outage shows it.

## The naive version

Read the code and trust that the exception handlers are right.

## What this project must deliver

- A payments service whose five dependencies are reached through Toxiproxy.
- Experiments with a steady state, an injected failure, an observation and a verdict.
- A first version with planted weaknesses and a hardened one.
- A monitor whose alert delay is counted in probes, and a scoreboard.
