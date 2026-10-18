# Session Guide — Passkeys and WebAuthn

## Learning Objectives

By the end of the session you can:

- explain what a passkey is and what the site stores
- list the checks a relying party makes on every answer, in order
- explain why a look-alike site gets nothing from a passkey
- explain user verification and the signature counter, and what synced passkeys change
- describe why recovery and old passwords become the weak points

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Registering a passkey: the bank keeps only a public key | 7 min |
| 0:17 | Act 2: Signing in: a fresh challenge, signed once | 7 min |
| 0:24 | Act 3: A look-alike site gets nothing it can use | 7 min |
| 0:31 | Act 4: User verification for payments, and counters against copies | 7 min |
| 0:38 | Act 5: The bill: recovery, lost devices and the move away from passwords | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the house key and the lock. Run the acts. After act one, ask what a thief who copies the bank's database could do with it. After act
three, ask the group to say exactly which of the three refusals would stop a real phishing page, and which only matters if the browser is faulty.
In act five, ask what the bank's recovery flow should require before it lets someone register a new passkey.

## Exercises

1. Add a second relying party, shop.example, and show that one authenticator holds separate keys for both, which cannot be used for each other.
2. Make the challenge lifetime one minute and write the boundary tests first.
3. Let a recovery code register a new passkey only after a waiting period and a message to the customer's other devices.
4. Add a check that a credential id is not longer than WebAuthn allows, and refuse it otherwise.
