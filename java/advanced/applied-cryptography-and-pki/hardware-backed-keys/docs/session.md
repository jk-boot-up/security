# Session Guide — Hardware-Backed Keys

## Learning Objectives

By the end of the session you can:

- generate a key on a PKCS#11 token that cannot be extracted
- sign from Java through the PKCS11 KeyStore
- say what a token protects and what it does not
- explain why wrapping a key for backup weakens it

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The key ceremony | 7 min |
| 0:17 | Act 2: Five ways out | 7 min |
| 0:24 | Act 3: PINs and a file for contrast | 7 min |
| 0:31 | Act 4: Rate, approvers and audit | 7 min |
| 0:38 | Act 5: A compromised host | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and read the five attempts to get the key out. Then read the compromised host section and find which of the bank's own controls the token does not enforce.

## Exercises

1. Make the release key non-extractable and see which backup step fails.
2. Add a per-key signing counter in SigningService and persist it.
3. Log out between batches and measure how many forged signatures fit in the window.
4. Add a second approver check on the PIN login.
