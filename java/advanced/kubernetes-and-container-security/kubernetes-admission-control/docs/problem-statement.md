# Problem Statement

## The scenario

A bank wants unsafe workloads refused when they are created, not found in a review afterwards.

## The naive version

Ask every team to follow a checklist, and look at the cluster now and then.

## What this project must deliver

- A real cluster with an admission controller.
- Policies that validate, mutate and verify signed images.
- Audit mode, policy reports and a rollout to deny.
- Exceptions that expire, and what happens when the controller is down.
