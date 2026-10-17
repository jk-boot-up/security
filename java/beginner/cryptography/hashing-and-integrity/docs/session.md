# Session Guide — Hashing and Integrity

## Learning Objectives

By the end of the session you can:

- explain the difference between a checksum, a hash and a keyed hash
- say why a hash stored beside the data protects nothing against an attacker
- say what has to be true for a plain hash to help
- name three problems that remain after using HMAC

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A checksum for accidents | 7 min |
| 0:17 | Act 2: A cryptographic hash: SHA-256 | 7 min |
| 0:24 | Act 3: A hash kept beside the data | 7 min |
| 0:31 | Act 4: A keyed hash: HMAC-SHA-256 | 7 min |
| 0:38 | Act 5: The bill: boundaries, repeats, and a shared key | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the wax seal analogy. Run the demo one act at a time. After act one, ask the group what the attacker needed in order to pass the check, and let them conclude that nothing
secret was involved. After act three, ask where the published hash should be kept, and why the shop's own website works but the mirror does not. In act five, ask the group how the
warehouse could be given the ability to check tags without the ability to make them, and keep that question for the signatures project.

## Exercises

1. Add a separator that cannot appear in the data to the JOINED encoding and test the boundary trick again.
2. Add a timestamp to the invoice and make the service refuse tags older than 5 minutes.
3. Write a test that fails if the HMAC check uses an ordinary string comparison.
4. Compute the SHA-256 of a file on your disk and compare it with the value your operating system's tool gives.
