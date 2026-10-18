# Spec — Race Conditions and Business Logic

## Purpose

Teach race conditions and business-logic flaws by showing two withdrawals and two coupon uses overlapping in a written order, fixing them with a lock and a compare-and-set, making retries idempotent, and enforcing business
rules and an order state machine on the server.

## Not in scope

- Database locking and isolation levels: the -with-postgres version.
- Distributed locks and consensus.
- Load testing.

## The problem

Developers test one request at a time, so overlapping requests and retries find the flaws in production.

## The pieces

| Role | Class |
| --- | --- |
| Requests as steps | `Steps` |
| Running steps in a written order | `Interleaving` |
| Shared state of the model | `Ledger`, `CouponBook` |
| Withdrawals | `NaiveWithdrawal`, `LockedWithdrawal`, `CasWithdrawal` |
| Coupons | `CouponRedemption` |
| Lock order | `LockingTransfer` |
| Real threads | `RealAccounts` |
| Idempotency and rules | `Bank` |
| States | `Order` |
| Prices | `Checkout` |
| Entry point | `RacesDemo` |

## Requirements

1. Some order of steps makes check-then-act overdraw, and one request after another does not.
2. With a lock or a compare-and-set, no order of steps overdraws or loses money.
3. A coupon claimed in one step is used once in every order.
4. The same idempotency key moves money once, even from many threads at once, and the same key with a different transfer is refused.
5. The bank refuses non-positive amounts, self-transfers, unknown accounts and overdrafts; checkout prices from the catalogue with one coupon; an order is refunded at most once and only after payment.
6. Opposite lock orders can deadlock; one fixed order never does.
7. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 24, all passing |
| Demo output | 69 lines, identical on every run |
| Acts | 5 |
| Video | 08:43, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
