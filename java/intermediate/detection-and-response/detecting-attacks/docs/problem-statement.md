# Problem Statement

## The scenario

A company logs every sign-in and admin change, and has no one who reads the logs. It wants alerts for the attacks that matter and few enough that they will be read.

## The naive version

Alert on every failed login, every login from a new place and every role change.

## What this project must deliver

- A synthetic day of logs with ground truth: noise, four attacks and the usual look-alikes.
- Five rules: brute force, spraying, impossible travel, success after failures and privilege change.
- Scoring: alerts, true and false positives and attacks noticed, per rule.
- A tuning experiment and a tuned rule set.
