# Session Guide — Session Management with Spring Session and Redis

## Learning Objectives

By the end of the session you can:

- explain what changes when sessions move from the server's memory to Redis
- show why Redis's keys are as sensitive as the cookie jar
- name the session rules Spring Session gives and the one it does not
- make the bank refuse when the store is down

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The session lives in Redis | 7 min |
| 0:17 | Act 2: A new id at every sign-in | 7 min |
| 0:24 | Act 3: How long a session lives | 7 min |
| 0:31 | Act 4: Signing out, everywhere, and the limit | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and ask who can read the keys. In act three ask where the absolute limit would live in your own bank. In act five ask what the bank should do while Redis is down for an hour.

## Exercises

1. Store a hash of the id as the Redis key, and see what Spring Session needs to change.
2. Add a step-up: ask for the password again before a payment, and rotate the id.
3. Make the idle timeout 15 minutes and the absolute timeout 8 hours, and test both.
4. Run a second Redis as a replica and decide what the bank does when the first one fails.
