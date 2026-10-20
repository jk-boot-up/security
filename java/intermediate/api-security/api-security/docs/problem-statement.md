# Problem Statement

## The scenario

A bank's mobile app uses a JSON API to update a profile, show an account and list the branch directory.

## The naive version

Copy the JSON onto the record, return the record, and take the page size from the caller: the app is ours, so it only sends what we expect.

## What this project must deliver

- A naive API with mass assignment, a whole-record response and an unlimited page size.
- An explicit request model with unknown fields refused.
- An explicit response model with a masked card, and a directory view for lists.
- A token-bucket rate limiter with a simulated clock, charging per row.
