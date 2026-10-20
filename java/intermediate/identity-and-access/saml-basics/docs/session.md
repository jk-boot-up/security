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
| 0:10 | Act 1: Signing in through the bank | 8 min |
| 0:18 | Act 2: A valid signature is not the same as the bank's signature | 8 min |
| 0:26 | Act 3: A valid signature on one assertion, and another one read | 8 min |
| 0:34 | Act 4: The statement itself is checked | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act two and ask whose key validated the self-signed response. In act three, point at the two assertions and ask which one the signature covers and which one the provider read. In act four, ask what each check would let an attacker do if it were missing.

## Exercises

1. Add a second wrapping variant: put the forged assertion after the genuine one, and see which providers read it.
2. Add a duplicate-ID variant with two elements with the same ID attribute, and watch the strict provider's count.
3. Add key rotation: let the strict provider pin two keys, and test with each.
4. Sign the response element too, and decide what that adds.
