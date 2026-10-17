# Problem Statement

## The scenario

An online store has staff, unattended jobs and services, all needing some access. It is easiest to give everyone, and every job, a broad role, and that makes every account worth stealing.

## The naive version

Give every job and developer the admin role so that nothing ever fails with "access denied", and leave temporary access in place once the work is done.

## What this project must deliver

- A policy engine with deny by default and explicit deny.
- Roles for a store, and a count of what each can reach.
- A blast radius comparison between an admin job and a scoped job.
- Separation of duties for refunds, even for an admin.
- Time-limited grants on a simulated clock.
- An audit of unused permissions, and owner-only file permissions.
