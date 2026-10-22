# Problem Statement

## The scenario

An online bank on Spring Boot that approves payments by asking a fraud-check service and moves money between accounts, where a developer wants the payment to keep flowing when something breaks.

## The naive version

Catch the exception and carry on, trust the transaction annotation, and turn on every detail in the error response.

## What this project must deliver

- A real Spring Boot bank and a real fraud-check server that fails in four ways.
- A decision that starts as no, with a time budget, one retry and a breaker.
- Four versions of one transfer, and the two transaction traps.
- One error handler, and a log that never holds a full card number.
