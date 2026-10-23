# Problem Statement

## The scenario

A bank wants injection flaws found in the source before the code runs.

## The naive version

Search the code for the word executeQuery and read every hit.

## What this project must deliver

- Rules written in Semgrep's YAML.
- A pattern rule and a taint rule, scored on planted flaws.
- Tests for the rules.
- Triage with reasons and owners.
