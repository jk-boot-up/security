# Session Guide — A Local Certificate Authority

## Learning Objectives

By the end of the session you can:

- explain what a certificate authority signs and what a signing request proves
- describe how path length and name constraints limit a CA
- explain why revocation depends on the client, and what failing closed means for a list
- compare short-lived certificates with revocation lists

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Running a certificate authority | 7 min |
| 0:17 | Act 2: Issuing from a signing request | 7 min |
| 0:24 | Act 3: Revocation: taking a certificate back | 7 min |
| 0:31 | Act 4: Short-lived certificates instead of revocation | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and ask what stops a stolen issuer from naming pay.example. In act three, ask which client is protected and which is not. In act four, ask what the store must automate to live with 24-hour certificates.

## Exercises

1. Add an OCSP-style status check and compare its failure modes with the list.
2. Add a profile for 7-day certificates and decide which services should use it.
3. Make the inventory send a warning 14 days before expiry and test it on the fixed clock.
4. Add a second issuing authority limited to a different domain and show that neither can name the other's domain.
