# Session Guide — Mutual TLS Between Services

## Learning Objectives

By the end of the session you can:

- explain what a client certificate proves and what it does not
- take a service identity from a certificate SAN and authorize by it
- renew a short-lived certificate and reload a server without downtime
- say what a revoked certificate can still do

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The identity in a certificate | 7 min |
| 0:17 | Act 2: Who gets through the handshake | 7 min |
| 0:24 | Act 3: Allowed by identity, not by the network | 7 min |
| 0:31 | Act 4: Rotation: a certificate that lives 8 seconds | 7 min |
| 0:38 | Act 5: A stolen certificate, and revocation | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and read the chain in the first act, then compare the third act with the second: the same host gets two answers. Open Identities.java and see which part of the certificate is the identity. Then read the rotation act and find where the server stops using the old certificate.

## Exercises

1. Add a service to the policy and issue it a certificate.
2. Make the ledger read the revocation list every 10 seconds on a timer and measure how long a revoked certificate still works.
3. Add name constraints to the second CA so it can only mint identities under its own prefix.
4. Check the revocation list inside the handshake with a custom trust manager instead of in the handler.
