# Problem Statement

## The scenario

An online store is growing. Every few weeks a feature ships, and each one adds
something an attacker could reach. There is a small security budget and a long
list of things that could go wrong, and nobody has written the list down.

## The naive version

The first approach is to fix whatever was in the news last week, or whatever the
loudest person asked for. There is no list of ways in, no way to compare a
leaked order page with a spammed review box, and no record of what was decided
and why.

## What this project must deliver

- An inventory of entry points, grouped by who can reach them, that grows when a feature ships.
- Risks written with an asset, a threat and a vulnerability, and scored as likelihood times impact.
- A ranking that is the same on every run, with a rule for ties.
- A planner that spends a fixed budget where it removes the most score.
- A demonstration that two reviewers can rank the same list differently.
- The same numbers every run, checked against a golden output file.
