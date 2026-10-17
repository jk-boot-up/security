# Session Guide — Certificates and the Chain of Trust

## Learning Objectives

By the end of the session you can:

- say what a certificate contains and what it proves
- follow a chain from a server to a trusted root
- list what a validator checks, and what it does not
- explain why the host name is a separate check
- describe what revocation is for, and its limits

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A chain of three certificates | 7 min |
| 0:17 | Act 2: The browser's check | 7 min |
| 0:24 | Act 3: Three reasons to refuse | 7 min |
| 0:31 | Act 4: Changed certificates, and certificates that overstep | 7 min |
| 0:38 | Act 5: Revocation, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the passport. Run the demo one act at a time. After act two, ask what the browser needed to know in advance. After act three, ask why the rogue root's matching name does not help the attacker.
In act five, ask who should be allowed to hold the intermediate's key, and where.

## Exercises

1. Add a subject alternative name for www.shop.example and check both names match.
2. Issue a certificate valid from a future date and see the reason the validator gives.
3. Add a second intermediate and a path length of one, and find where the validator stops.
4. Make the revocation list a signed document, and have the validator check its signature.
