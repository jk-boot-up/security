# Problem Statement

## The scenario

A bank wants every release of its ledger library to come out of a clean build, to be signed, to say where it came from and to pass checks that cannot be skipped by a tool that is missing.

## The naive version

Build on a laptop, run the checks if the tools are there, and copy the jar.

## What this project must deliver

- A reproducible build.
- Signatures and provenance that are verified.
- Gates that fail closed.
- A deploy step that checks everything again.
