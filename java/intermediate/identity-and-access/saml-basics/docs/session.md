# Session Guide — SAML Single Sign-On and Signature Wrapping

## Learning Objectives

By the end of the session you can:

- explain why the key in a signature cannot be the key you trust
- describe signature wrapping and why checking one element and reading another is the bug
- list what a service provider must check in an assertion besides the signature
- say why a vetted library is the production answer

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Signing in through the bank | 7 min |
| 0:17 | Act 2: How the bank signs the assertion | 7 min |
| 0:24 | Act 3: A valid signature is not the same as the bank's signature | 7 min |
| 0:31 | Act 4: Signature wrapping: one assertion checked, another read | 7 min |
| 0:38 | Act 5: The statement itself is checked | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and follow the five signing steps with the printed byte counts. In act three, ask whose key validated the self-signed response. In act four, point at the two assertions and ask which one the signature covers and which one the provider read. In act five, ask what each check would let an attacker do if it were missing.

## Exercises

1. Add a second wrapping variant: put the forged assertion after the genuine one, and see which providers read it.
2. Add a duplicate-ID variant with two elements with the same ID attribute, and watch the strict provider's count.
3. Add key rotation: let the strict provider pin two keys, and test with each.
4. Sign the response element too, and decide what that adds.
