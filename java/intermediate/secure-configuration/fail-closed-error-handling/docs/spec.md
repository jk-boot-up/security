# Spec — Failing Closed: Secure Error Handling

## Purpose

Teach failing closed by breaking a bank's payment check, transfer and error page in each way that real dependencies fail, on a simulated clock, and rebuilding each to refuse, undo, release and stay quiet.

## Not in scope

- A resilience library: the tools version.
- Distributed transactions across systems.
- Logging and alerting pipelines: the detection-and-response module.

## The problem

Security checks are tested for the case where everything works; the failure cases are where requests slip through, money is lost and internals leak.

## The pieces

| Role | Class |
| --- | --- |
| Simulated clock | `SimClock` |
| Fraud service with faults | `FraudService` |
| Failing open | `NaiveAuthorizer` |
| Failing closed | `SafeAuthorizer` |
| Transfers | `Ledger`, `Transfers` |
| Connections | `ConnectionPool` |
| Error pages and log | `ErrorPages`, `SecurityLog` |
| Breaker and retry | `CircuitBreaker`, `ResilientAuthorizer` |
| Entry point | `FailClosedDemo` |

## Requirements

1. The naive check approves when the fraud check is down and skips the limit on a bad value.
2. The safe check refuses on any error, timeout or unreadable value, with a reference and a log line.
3. An atomic transfer leaves the total unchanged on failure; try-with-resources leaks no connection.
4. The safe page has no internals, and the log has no full card number.
5. An open breaker refuses without calling the service, and closes after a successful trial.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 17, all passing |
| Demo output | 32 lines, identical on every run |
| Acts | 5 |
| Video | 08:07, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
