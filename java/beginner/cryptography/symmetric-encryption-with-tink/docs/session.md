# Session Guide — Symmetric Encryption with Google Tink

## Learning Objectives

By the end of the session you can:

- name what Tink decides for the caller that plain AES-GCM leaves to the caller
- explain a keyset, the primary key, and how rotation works
- explain envelope encryption, and what it moves to the master key
- name three costs of using a library like Tink

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: One call, and no nonce to look after | 7 min |
| 0:17 | Act 2: Any change is refused | 7 min |
| 0:24 | Act 3: Replacing the key without losing the data | 7 min |
| 0:31 | Act 4: Keys that stay at home: envelope encryption | 7 min |
| 0:38 | Act 5: The bill: what Tink decides for you, and what it does not | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the plain project first, then this one, and compare the first act: the same story, with no nonce in the code. Open `Bank` and find the line where the plain version had a nonce
source. Then rotate the key by hand in the debugger, step by step, and watch the key id at the start of each message. In act four, ask the group where the master key should live, and who
should be able to read it.

## Exercises

1. Switch the template from AES256_GCM to XCHACHA20_POLY1305 and rerun. What changes in the demo's numbers?
2. Add a third key and make it primary. How many keys does the keyset hold, and which messages still open?
3. Disable the oldest key without deleting it, and see what Tink does when an old message arrives.
4. Make the master key wrap the data key together with the account as associated data, and test a wrapped key moved to another account.
