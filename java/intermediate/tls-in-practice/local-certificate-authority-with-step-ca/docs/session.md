# Session Guide — A Local Certificate Authority with step-ca

## Learning Objectives

By the end of the session you can:

- start a CA and give a client its root
- get a certificate with a token and with ACME
- renew a certificate and explain who may
- say what revocation covers and what it does not

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A certificate authority that runs | 7 min |
| 0:17 | Act 2: A certificate for a service, with a token | 7 min |
| 0:24 | Act 3: A certificate for a service, with ACME | 7 min |
| 0:31 | Act 4: Renewing before it ends | 7 min |
| 0:38 | Act 5: Revocation, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and find the three ways a token request is refused. In act four ask why only the key's holder can renew. In act five find the client that accepts a revoked certificate.

## Exercises

1. Add a name constraint to the issuing authority and issue for a name outside it.
2. Run step ca renew --daemon and stop it, then watch the certificate expire.
3. Add a second ACME provisioner with a different policy.
4. Serve the store behind nginx with these certificates.
