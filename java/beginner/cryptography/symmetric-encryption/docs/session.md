# Session Guide — Symmetric Encryption (AES-GCM)

## Learning Objectives

By the end of the session you can:

- explain what a symmetric key, a block cipher and a mode are
- say why ECB leaks patterns
- say what authenticated encryption adds
- explain why a nonce must never repeat, and what an attacker does with a repeat

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A statement stored as it is | 7 min |
| 0:17 | Act 2: AES in ECB mode: every block on its own | 7 min |
| 0:24 | Act 3: AES-GCM: hiding and checking | 7 min |
| 0:31 | Act 4: The one mistake: using a nonce twice | 7 min |
| 0:38 | Act 5: The bill: sizes, nonces, and the key | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the lock box analogy. Run the demo one act at a time. After act two, ask the group what the attacker has learnt, without reading any line. After
act three, ask what the associated data protects against, and let them name moving a message between accounts. In act four, ask what the attacker needed, and make the
point that the key was never involved. In act five, ask where the key should live and who should be able to read it.

## Exercises

1. Make the statement 12 lines instead of 8 and count the distinct blocks again.
2. Seal a message with `RandomNonces` and check that two runs of the program give different cipher texts.
3. Change the associated data to include the date, and open a message with an old date.
4. Write a test that fails if two messages sealed by one `GcmBox` ever get the same nonce.
