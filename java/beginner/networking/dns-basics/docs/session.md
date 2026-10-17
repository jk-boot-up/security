# Session Guide — DNS Basics for Security

## Learning Objectives

By the end of the session you can:

- describe what a DNS question and answer contain
- explain what a cache and a TTL do, and what that means for a false answer
- explain how a transaction id and a source port protect a query
- list what limits the damage of a poisoned answer

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A DNS message, byte by byte | 7 min |
| 0:17 | Act 2: The cache and the time to live | 7 min |
| 0:24 | Act 3: A real exchange over UDP | 7 min |
| 0:31 | Act 4: Cache poisoning by guessing | 7 min |
| 0:38 | Act 5: How long a false answer lives, and what stops it | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with directory enquiries. Run the demo one act at a time. After act three, ask what a person on the path can read. In act four, ask why the port helps as well as the id. After act five, ask which defence stops a poisoning by someone on the path, and which does not.

## Exercises

1. Make the attacker send 10000 guesses instead of 1000 and check the keyed resolver again.
2. Add a second record type (a text record) to the message codec.
3. Add a negative cache, so that a missing name is remembered for a short time, and think about how it could be abused.
4. Change the cap on lifetimes and find the shortest lifetime that still gives a useful cache.
