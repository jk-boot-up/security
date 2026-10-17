# Session Guide — Webhook Security: Signatures, Replays and Idempotency

## Learning Objectives

By the end of the session you can:

- explain why a message to a public address proves nothing on its own
- verify an HMAC signature over raw bytes, in constant time
- explain why the time must be signed, and how a window and an event record stop replays
- make a handler idempotent and safe for events out of order
- rotate a shared secret without refusing real events

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: An endpoint that trusts whatever is posted | 7 min |
| 0:17 | Act 2: A signature over the exact bytes | 7 min |
| 0:24 | Act 3: A real message sent again | 7 min |
| 0:31 | Act 4: Twice, out of order, and a new secret | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the letterbox. Run act one and ask what the store would ship. In act two, ask why the body must be the raw bytes. In act three, ask why the signature alone did not stop the
second refund. In act four, ask what the provider does after a 409. In act five, ask which events are worth a call to the provider's API.

## Exercises

1. Add a third event type, payment.disputed, with the state changes it allows.
2. Keep the processed event ids for a limited time, and find the shortest time that is still safe given the provider's retry schedule.
3. Make the receiver accept a header with two v1 values, as some providers send during rotation.
4. Move the record of processed events into a store shared by two receivers, and show that a replay to the second one is still caught.
