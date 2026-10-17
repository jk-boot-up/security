# Problem Statement

## The scenario

An online store learns about payments and refunds from webhooks that a payment provider posts to a public address. The store ships goods and pays refunds based on them.

## The naive version

Trust any well-formed event posted to the webhook address, and act on every one that arrives.

## What this project must deliver

- An endpoint that verifies an HMAC-SHA256 signature over the signed time and the exact raw body, in constant time.
- Replay protection with a signed time window and a record of processed event ids.
- Idempotent handlers with state checks, and retryable answers for events that arrive out of order.
- Secret rotation with two valid secrets during a changeover.
- The same checks on a real local HTTP server, with a size limit and POST only.
