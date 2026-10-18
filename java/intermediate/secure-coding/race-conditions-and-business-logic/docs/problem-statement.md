# Problem Statement

## The scenario

An online bank and store serve many requests at once, from many devices. Customers double-click, apps retry when a reply is lost, and attackers send the same request many times in parallel on purpose.

## The naive version

Write each operation as it reads: check the balance, then take the money; check the coupon, then apply it; mark the order refunded; trust the amount and the total the client sends. Every line is right, and the
combination is wrong as soon as two requests overlap.

## What this project must deliver

- A step-by-step model that shows exactly how two requests overlap, in a written and repeatable order.
- A lock and a compare-and-set that keep the balance right in every order, checked exhaustively.
- Idempotency keys that make a retried payment run once.
- Server-side business rules: positive amounts, no self-transfers, catalogue prices, one coupon, and an order state machine.
- Real-thread tests that force the bad order and check the fixes under load.
- The costs: waiting, deadlock and the limits of a lock in one process.
