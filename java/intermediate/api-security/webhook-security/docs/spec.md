# Spec — Webhook Security: Signatures, Replays and Idempotency

## Purpose

Teach how to receive webhooks safely by forging and replaying events against an online store's endpoint, then verifying signatures over raw bytes, refusing replays, handling
retries and disorder idempotently, and rotating the shared secret.

## Not in scope

- Sending webhooks: the provider side is only simulated.
- Asymmetric signatures on webhooks, which some providers use: the digital-signatures project covers the idea.
- A message queue or a database for the processed events: the record is in memory.

## The problem

Webhook endpoints are public, and teams often act on any well-formed message, verify a parsed copy instead of the raw bytes, or forget replays and retries.

## The pieces

| Role | Class |
| --- | --- |
| Signature format and checks | `Signatures` |
| The event and its strict parser | `Event` |
| Orders and refunds | `Orders` |
| The endpoint's checks | `Receiver` |
| The local HTTP endpoint | `WebhookServer` |
| The simulated provider | `Provider` |
| Entry point | `WebhookDemo` |

## Requirements

1. An unsigned, wrongly signed or changed message changes nothing.
2. The HMAC matches RFC 4231 test case two, and the header matches an independently computed value.
3. Messages outside the time window are refused, and a processed event is not applied again.
4. An event that arrives before its order is ready is answered 409 and applied on a later retry.
5. Two secrets work during a changeover, and only the new one after.
6. The demo prints the same output on every run.

## Measured

| What | Value |
| --- | --- |
| Language and build | Java 25, Gradle wrapper |
| Tests | 16, all passing |
| Demo output | 37 lines, identical on every run |
| Acts | 5 |
| Video | 08:06, 1920×1080, narrated in the amy-slow voice |

## Files every project has

`README.md` and `README.html`, `docs/` with the problem statement, prerequisites, session guide, explained text, four diagram pages, threat model, spec, expected output, YouTube sheet, thumbnail and `animation.html`; `concept.toml`; `video/` with the scene script and voice settings.
