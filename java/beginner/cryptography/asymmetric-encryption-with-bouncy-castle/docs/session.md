# Session Guide — Asymmetric Encryption with Bouncy Castle and OpenSSL

## Learning Objectives

By the end of the session you can:

- name what Bouncy Castle adds to the JDK for asymmetric encryption
- read the header of a PEM file and say what it holds
- say what a CMS envelope contains, and why a standard format matters
- explain why a self-signed certificate proves nothing by itself

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Keys as PEM text | 7 min |
| 0:17 | Act 2: A certificate for the public key | 7 min |
| 0:24 | Act 3: A standard envelope: CMS EnvelopedData | 7 min |
| 0:31 | Act 4: A different program opens it | 7 min |
| 0:38 | Act 5: The bill: what Bouncy Castle costs | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the plain project first, then this one, and compare the third act with the plain third act: both are hybrid encryption, and this one is in a standard file format. Then run
`openssl cms -decrypt` by hand on a file the demo wrote, to see it for yourself. In act five, ask the group what other places an "easy call" with a weaker default might hide in code they know.

## Exercises

1. Seal an envelope for two recipients at once, and open it with each of their keys.
2. Make the certificate valid for 90 days instead of a year, and write the test first.
3. Read the PEM file for a key made by `openssl genrsa` and use it in the demo.
4. Switch the content encryption to an authenticated mode and see what CMS type you need.
