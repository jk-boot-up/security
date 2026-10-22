# Problem Statement

## The scenario

A bank lets a customer withdraw from the same account on two devices at the same moment, and the rule is that the balance never goes below zero.

## The naive version

Read the balance, check it, write the new one.

## What this project must deliver

- A real PostgreSQL and real concurrent sessions.
- Constraints, conditional updates, row locks, isolation levels and versions.
- Retries, deadlocks and waits as part of the design.
- A Hibernate version column and a one-time key.
