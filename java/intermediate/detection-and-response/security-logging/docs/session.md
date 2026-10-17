# Session Guide — Security Logging

## Learning Objectives

By the end of the session you can:

- list the fields a security event needs, and the event types a bank must log
- name the values that must never be logged, and two ways to keep them out
- explain log injection and why structured output prevents it
- explain what a hash chain detects, what it cannot, and why logs must leave the host

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What a security log must answer | 7 min |
| 0:17 | Act 2: What never to log | 7 min |
| 0:24 | Act 3: Text that tries to write the log | 7 min |
| 0:31 | Act 4: A log that shows it was changed | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the visitor book. Run the demo one act at a time. After act one, ask the group what other questions an investigator would ask. After act two, ask where else in their own systems a token could end up in a log. In act three, ask what
a forged line could make an investigator believe. In act four, ask who could still rewrite the log, and where the published head should live.

## Exercises

1. Add an event type for changes to a customer's contact details and log it from a new demo step.
2. Add a redaction rule for a national id number and a test that feeds one through every field.
3. Make the chain checker report every broken line, not only the first.
4. Sign the published head with an Ed25519 key, as in the digital-signatures project, and verify it before trusting the chain.
