# Problem Statement

## The scenario

An online store's account has jobs that need access to a few things: a nightly export, a support tool and a deploy pipeline. Someone has to write what each may do, and the easy way is a wildcard.

## The naive version

Give every job `Action: *` on `Resource: *`. It works on the first day, and a stolen key then opens everything.

## What this project must deliver

- Five IAM policies written as Terraform and applied to a local emulator.
- An evaluator in Java with deny by default, explicit deny, wildcards and boundaries.
- A blast-radius count for each identity over a fixed catalogue of 25 things.
- A linter for wildcards, NotAction and escalation.
- A check that what the emulator stores equals what the Java code reads.
