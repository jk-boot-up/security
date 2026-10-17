# Problem Statement

## The scenario

An online bank approves card payments with the help of a fraud-check service and a daily limit, and moves money between accounts. Every one of those dependencies fails sometimes: it is slow,
it is down, or it returns something the code cannot read.

## The naive version

Write the checks for the happy path, and wrap them in a catch-all so that a flaky dependency does not block customers. The catch-all turns every failure into an approval.

## What this project must deliver

- A payment check that approves by accident, and the same check rebuilt to start from no.
- Time budgets that turn a slow answer into a refusal.
- A transfer that is all or nothing, and connections released on every path.
- A safe error page with a reference, and a log that keeps the detail without the card number.
- A circuit breaker that refuses fast, a limited retry, and an alert on refusals.
- The same output on every run.
