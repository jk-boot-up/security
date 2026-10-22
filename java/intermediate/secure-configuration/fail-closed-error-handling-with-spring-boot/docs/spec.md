# Spec — Failing Closed with Spring Boot

## Purpose

Teach failing closed in Spring Boot: decisions that start as no, bounded calls, transaction rules and error responses.

## Not in scope

- A full resilience library.
- Distributed transactions across systems.
- Real payment processing.

## The problem

Teams catch exceptions to keep requests flowing, trust a transaction annotation without its rules, and leave debugging details in error responses.

## The pieces

| Role | Class |
| --- | --- |
| Payments | `OpenPaymentService`, `ClosedPaymentService`, `PaymentController` |
| Fraud call | `FraudClient`, `Breaker`, `BankConfig` |
| Transfers | `Ledger`, `Transfers` |
| Errors | `ErrorHandling`, `SecurityLog`, `References` |
| Test double | `FraudServer` |
| Entry point | `FailClosedSpringDemo` |

## Requirements

1. The open version approves a risky payment when the check is broken.
2. The closed version refuses on every kind of broken check, with a reference.
3. A slow check is cut off at its time budget.
4. A breaker stops calls to a failing service and closes after the cool-down.
5. Only a unit of work with rollbackFor, called from outside, undoes a failed transfer.
6. The error response gives no server, card or code details, and the log has the last four digits only.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 15, all passing |
| Demo output | 36 lines, identical on every run |
| Acts | 5 |
| Video | 07:01, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
